#version 330 core
uniform sampler2D EmissionSampler;
uniform sampler2D BloomSampler;
uniform float Strength;
in vec2 uv;
out vec4 outColor;

void main() {
    vec4 emission = texture(EmissionSampler, uv);
    vec3 bloom = texture(BloomSampler, uv).rgb;
    // Preserve detail in the fullbright strip itself; the halo adds light outside it.
    outColor = vec4(bloom * Strength * (1.0 - emission.a * 0.65), 0.0);
}
