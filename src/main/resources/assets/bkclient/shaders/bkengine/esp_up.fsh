#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

uniform vec4 params; // halfPixelX, halfPixelY, offset, padding

// Dual-Kawase upsample: 8 taps on a diamond, offset scales the glow radius.
void main() {
    vec2 hp = params.xy * params.z;

    vec4 sum = texture(Sampler0, texCoord + vec2(-hp.x * 2.0, 0.0));
    sum += texture(Sampler0, texCoord + vec2(-hp.x,  hp.y)) * 2.0;
    sum += texture(Sampler0, texCoord + vec2( 0.0,   hp.y * 2.0));
    sum += texture(Sampler0, texCoord + vec2( hp.x,  hp.y)) * 2.0;
    sum += texture(Sampler0, texCoord + vec2( hp.x * 2.0, 0.0));
    sum += texture(Sampler0, texCoord + vec2( hp.x, -hp.y)) * 2.0;
    sum += texture(Sampler0, texCoord + vec2( 0.0,  -hp.y * 2.0));
    sum += texture(Sampler0, texCoord + vec2(-hp.x, -hp.y)) * 2.0;

    fragColor = vec4((sum * 0.0833).rgb, 1.0);
}
