#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

uniform vec4 params; // halfPixelX, halfPixelY, offset, padding

// Dual-Kawase downsample: 5 taps, weight 4/1/1/1/1.
void main() {
    vec2 hp = params.xy;

    vec4 sum = texture(Sampler0, texCoord) * 4.0;
    sum += texture(Sampler0, texCoord - hp.xy);
    sum += texture(Sampler0, texCoord + hp.xy);
    sum += texture(Sampler0, texCoord + vec2(hp.x, -hp.y));
    sum += texture(Sampler0, texCoord - vec2(hp.x, -hp.y));

    fragColor = vec4((sum * 0.125).rgb, 1.0);
}
