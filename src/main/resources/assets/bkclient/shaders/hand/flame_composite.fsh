#version 330 core
// BurnData flattened for the BK 1.21.4 shader wrapper.

uniform vec4 Screen;
uniform vec4 GlowParams;
uniform vec4 TrailParams;
uniform vec4 FireParams;

uniform sampler2D MaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D TrailSampler;

in vec2 uv;
out vec4 outColor;

float sampleMask(vec2 coord) {
    return clamp(texture(MaskSampler, coord).a, 0.0, 1.0);
}

void main() {
    vec2 texelSize = 1.0 / max(Screen.xy, vec2(1.0));
    vec4 glow = texture(GlowSampler, uv);
    vec4 trail = texture(TrailSampler, uv);
    float handMask = sampleMask(uv);
    float outsideMask = 1.0 - smoothstep(0.015, 0.20, handMask);
    float fireGate = mix(outsideMask, 1.0, clamp(Screen.w, 0.0, 1.0));
    float neighborAlpha = 0.0;
    neighborAlpha = max(neighborAlpha, sampleMask(uv + vec2(texelSize.x, 0.0)));
    neighborAlpha = max(neighborAlpha, sampleMask(uv - vec2(texelSize.x, 0.0)));
    neighborAlpha = max(neighborAlpha, sampleMask(uv + vec2(0.0, texelSize.y)));
    neighborAlpha = max(neighborAlpha, sampleMask(uv - vec2(0.0, texelSize.y)));
    float objectEdge = clamp(max(glow.a * 1.12, neighborAlpha) * fireGate, 0.0, 1.0);

    float trailVisibility = clamp(0.88 + objectEdge * 0.32, 0.0, 1.0) * fireGate;
    vec3 effect = trail.rgb * trailVisibility * TrailParams.w * 0.86;
    float glowAlpha = clamp(glow.a * (0.94 + objectEdge * 0.32) * fireGate, 0.0, 0.76);
    effect += glow.rgb * glowAlpha * (0.72 + FireParams.a * 0.26);
    outColor = vec4(clamp(effect, 0.0, 1.0), 0.0);
}
