#version 150

#moj_import <bkclient:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float CornerSmoothness;
uniform float GlobalAlpha;
uniform float Selection;
uniform float SelectionProgress;

uniform float FresnelPower;
uniform vec3 FresnelColor;
uniform float FresnelAlpha;
uniform float BaseAlpha;
uniform bool FresnelInvert;
uniform float FresnelMix;
uniform float DistortStrength;
uniform float GlareStrength;
uniform vec2 ScreenSize;
uniform float ChromaticStrength;

uniform int JoinedPanel;
uniform vec4 JoinedHeader;
uniform vec2 JoinedBounds;
uniform vec4 AccentColor;
uniform vec4 Pill;
uniform vec3 Bubble;
uniform float Merge;
uniform vec4 PanelColor;
uniform float GlassMix;
out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

float sdfUnified(vec2 p, vec2 b, float r, float smoothness) {
    vec2 q = abs(p) - b + r;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r;
}

float islandSDF(vec2 p) {
    vec4 bodyRadius = Radius;
    vec4 headerRadius = vec4(min(Radius.x, min(JoinedHeader.z, JoinedHeader.w)));
    if (JoinedPanel == 1) {
        // Corner order in roundedBoxSDF: bottom-right, top-right, bottom-left, top-left.
        // At a shared outer edge the touching corners must be square; rounding both
        // pieces creates a notch even after the smooth union and outer bounds clamp.
        if (abs((JoinedHeader.x - JoinedHeader.z) - (Pill.x - Pill.z)) < 0.001) {
            bodyRadius.w = 0.0;
            headerRadius.z = 0.0;
        }
        if (abs((JoinedHeader.x + JoinedHeader.z) - (Pill.x + Pill.z)) < 0.001) {
            bodyRadius.y = 0.0;
            headerRadius.x = 0.0;
        }
    }
    float a = roundedBoxSDF(p - Pill.xy, Pill.zw, bodyRadius, CornerSmoothness);
    float b = JoinedPanel == 1
        ? roundedBoxSDF(p - JoinedHeader.xy, JoinedHeader.zw, headerRadius, CornerSmoothness)
        : length(p - Bubble.xy) - Bubble.z;
    float k = max(Merge, 0.0001);
    float h = max(k - abs(a - b), 0.0) / k;
    float shape = min(a, b) - h * h * k * 0.25;
    // Keep aligned headers inside the same outer boundary as the list.
    if (JoinedPanel == 1) shape = max(shape, roundedBoxSDF(p, JoinedBounds, Radius, CornerSmoothness));
    return shape;
}

void main() {
    vec2 center = Size * 0.5;
    vec2 box_half_size = Pill.zw;
    vec2 pos = (FragCoord * Size) - center;

    float distance = islandSDF(pos);
    float alpha = 1.0 - smoothstep(-Smoothness, Smoothness, distance);

    float rMax = max(max(Radius.x, Radius.y), max(Radius.z, Radius.w));
    float eps = 2.0;
    float sdfC = islandSDF(pos);
    float sdfRx = islandSDF(pos + vec2(eps, 0.0));
    float sdfRy = islandSDF(pos + vec2(0.0, eps));
    vec2 grad = vec2(sdfRx - sdfC, sdfRy - sdfC);
    float gradLen = length(grad);
    vec2 edgeNormal = gradLen > 0.001 ? grad / gradLen : vec2(0.0);

    float sdfVal = islandSDF(pos);

    float nmerged = -sdfVal;
    // Keep refraction proportional on compact HUD pills. The old minimum of 15 px
    // covered nearly the entire 20 px Armor panel and looked like an external glow.
    float refThickness = clamp(min(box_half_size.x, box_half_size.y) * 0.35, 2.0, 15.0);
    float xR = clamp(1.0 - nmerged / max(refThickness, 1.0), 0.0, 1.0);
    float thetaI = asin(xR * xR);
    float ior = 1.0 + DistortStrength * 4.0;
    float sinThetaT = clamp(sin(thetaI) / max(ior, 1.001), -1.0, 1.0);
    float thetaT = asin(sinThetaT);
    float edgeFactor = -tan(thetaT - thetaI);
    float refFade = 1.0 - smoothstep(refThickness * 0.25, refThickness, nmerged);
    edgeFactor *= refFade;

    vec2 refrPx = -edgeNormal * edgeFactor * DistortStrength * 90.0;
    vec2 refrOffset = vec2(refrPx.x / ScreenSize.x, -refrPx.y / ScreenSize.y);

    vec2 baseTexCoord = clamp(TexCoord + refrOffset, 0.0, 1.0);

    vec4 texColor;
    if (ChromaticStrength > 0.001) {
        vec2 chromOffset = refrOffset * ChromaticStrength;
        vec4 texR = texture(Sampler0, clamp(baseTexCoord + chromOffset, 0.0, 1.0));
        vec4 texG = texture(Sampler0, baseTexCoord);
        vec4 texB = texture(Sampler0, clamp(baseTexCoord - chromOffset, 0.0, 1.0));
        texColor = vec4(texR.r, texG.g, texB.b, texG.a) * FragColor;
    } else {
        texColor = texture(Sampler0, baseTexCoord) * FragColor;
    }

    float seamBlend = smoothstep(refThickness * 0.5, refThickness, nmerged);
    if (seamBlend > 0.001) {
        vec3 seamOrigin = texture(Sampler0, TexCoord).rgb * FragColor.rgb;
        texColor.rgb = mix(texColor.rgb, seamOrigin, seamBlend);
    }

    float fresnelRange = clamp(min(box_half_size.x, box_half_size.y) * 0.5, 2.0, 30.0);
    float edge_gradient = 1.0 - smoothstep(0.0, fresnelRange, -sdfVal);

    float fresnel;
    float base = FresnelInvert ? edge_gradient : (1.0 - edge_gradient);

    if (FresnelPower > 20.0) {
        fresnel = exp(FresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, FresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec3 finalColor = mix(texColor.rgb, FresnelColor, fresnel * FresnelMix);
    float finalAlpha = mix(BaseAlpha, FresnelAlpha, fresnel) * alpha;

    if (finalAlpha < 0.001) {
        discard;
    }

    if (GlareStrength > 0.001) {
        float edgeDist = abs(islandSDF(pos));
        float edgeGlow = exp(-edgeDist * edgeDist * 10.0);
        float angle = atan(pos.y, pos.x);
        if (angle < 0.0) angle += 6.2831853;
        float diff1 = abs(angle - 0.785398);
        float diff2 = abs(angle - 3.92699);
        float angleDiff = min(diff1, diff2);
        angleDiff = min(angleDiff, 6.2831853 - angleDiff);
        float angleGlow = exp(-angleDiff * angleDiff * 1.5);
        float waterGlare = edgeGlow * angleGlow * GlareStrength * alpha;
        finalColor += vec3(waterGlare);
        finalAlpha = max(finalAlpha, waterGlare * 0.5);
    }

    float glassAlpha = clamp(finalAlpha * GlassMix, 0.0, 1.0);
    float fillAlpha = PanelColor.a * alpha;
    float combinedAlpha = fillAlpha + glassAlpha * (1.0 - fillAlpha);
    vec3 combined = (PanelColor.rgb * fillAlpha + finalColor * glassAlpha * (1.0 - fillAlpha)) / max(combinedAlpha, 0.0001);
    float accentAlpha = AccentColor.a * clamp(1.0 - FragCoord.x / .69, 0.0, 1.0) * alpha;
    combined = mix(combined, AccentColor.rgb, accentAlpha);
    combinedAlpha += accentAlpha * (1.0 - combinedAlpha);
    // Original diagonal white sweep, clipped by the panel's joined silhouette.
    float diag = (FragCoord.x + FragCoord.y) * 0.5;
    float sweepMask = smoothstep(SelectionProgress-.5, SelectionProgress, diag)
        * (1.0-smoothstep(SelectionProgress, SelectionProgress+.5, diag));
    float highlight = clamp(Selection,0.0,1.0) * (100.0/255.0) * sweepMask * alpha;
    float highlightedAlpha = highlight + combinedAlpha*(1.0-highlight);
    combined = (vec3(highlight) + combined*combinedAlpha*(1.0-highlight)) / max(highlightedAlpha,.0001);
    combinedAlpha = highlightedAlpha;
    OutColor = vec4(combined, combinedAlpha * GlobalAlpha);
}
