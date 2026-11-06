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

void main() {
    vec2 center = Size * 0.5;
    vec2 box_half_size = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    float distance = roundedBoxSDF(-pos, box_half_size, Radius, CornerSmoothness);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    float rMax = max(max(Radius.x, Radius.y), max(Radius.z, Radius.w));
    float eps = 2.0;
    float sdfC = sdfUnified(pos, box_half_size, rMax, CornerSmoothness);
    float sdfRx = sdfUnified(pos + vec2(eps, 0.0), box_half_size, rMax, CornerSmoothness);
    float sdfRy = sdfUnified(pos + vec2(0.0, eps), box_half_size, rMax, CornerSmoothness);
    vec2 grad = vec2(sdfRx - sdfC, sdfRy - sdfC);
    float gradLen = length(grad);
    vec2 edgeNormal = gradLen > 0.001 ? grad / gradLen : vec2(0.0);

    float sdfVal = sdfUnified(pos, box_half_size, rMax, CornerSmoothness);

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
        float edgeDist = abs(roundedBoxSDF(-pos, box_half_size, Radius, CornerSmoothness));
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

    OutColor = vec4(finalColor, finalAlpha * GlobalAlpha);
}
