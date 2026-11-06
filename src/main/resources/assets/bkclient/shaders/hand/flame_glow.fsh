#version 330 core
// BurnData flattened for the BK 1.21.4 shader wrapper.

uniform vec4 Screen;
uniform vec4 GlowParams;
uniform vec4 TrailParams;
uniform vec4 FireParams;

uniform sampler2D HandSampler;
uniform sampler2D MaskSampler;

in vec2 uv;
out vec4 outColor;

// Keep the flame halo detailed while limiting the per-pixel sampling cost.
// The previous 6x12 kernel made Flame dominate the GPU at high FPS/resolution.
const int RINGS = 4;
const int DIRS = 8;
const float TAU = 6.28318530718;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

vec3 itemColor(vec3 color) {
    color = clamp(color, 0.0, 1.0);
    float mx = max(max(color.r, color.g), color.b);
    if (mx < 0.04) return vec3(0.0);
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    float sat = mx - min(min(color.r, color.g), color.b);
    vec3 vivid = mix(vec3(gray), color, 1.0 + sat * 0.45);
    return clamp(vivid / max(max(max(vivid.r, vivid.g), vivid.b), 0.20), 0.0, 1.0)
        * min(mx * 1.18, 1.0);
}

void main() {
    vec2 texelSize = 1.0 / max(Screen.xy, vec2(1.0));
    float centerAlpha = texture(MaskSampler, uv).a;
    float outsideMask = 1.0 - smoothstep(0.015, 0.22, centerAlpha);
    float flowTime = Screen.z * GlowParams.z;
    float energy = 0.0;
    float colorWeight = 0.0;
    vec3 colorSum = vec3(0.0);

    for (int ring = 1; ring <= RINGS; ring++) {
        float rp = float(ring) / float(RINGS);
        float distancePx = GlowParams.x * rp;
        float ringWeight = pow(1.0 - rp * 0.86, 1.45);
        for (int dir = 0; dir < DIRS; dir++) {
            float dp = float(dir) / float(DIRS);
            float warp = noise(uv * 9.0 + vec2(float(ring) * 3.4, float(dir) * 2.2 + flowTime * 0.16));
            float angle = dp * TAU + (warp - 0.5) * 0.34;
            float dist = distancePx * (0.90 + warp * 0.22);
            vec2 offset = vec2(cos(angle), sin(angle)) * texelSize * dist;
            float sampleAlpha = clamp(texture(MaskSampler, uv + offset).a, 0.0, 1.0);
            vec3 sampleColor = texture(HandSampler, uv + offset).rgb;
            float weight = sampleAlpha * ringWeight;
            energy += weight;
            colorSum += sampleColor * weight;
            colorWeight += weight;
        }
    }

    energy = energy / float(RINGS * DIRS) * 4.25;
    float softHalo = smoothstep(0.018, 0.58, energy);
    float edgeNoise = noise(uv * vec2(15.0, 21.0) + vec2(flowTime * 0.20, -flowTime * 0.34));
    float alpha = softHalo * outsideMask * GlowParams.y * (0.86 + edgeNoise * 0.18);
    alpha = clamp(alpha, 0.0, 0.72);

    vec3 color = itemColor(colorSum / max(colorWeight, 0.001));
    color = mix(color, FireParams.rgb, clamp(GlowParams.w, 0.0, 1.0));
    outColor = vec4(color * (0.88 + alpha * 0.42), alpha);
}
