#version 330 core
uniform sampler2D SourceSampler;
uniform vec2 Direction;
in vec2 uv;
out vec4 outColor;

void main() {
    vec4 sum = texture(SourceSampler, uv) * 0.227027;
    sum += texture(SourceSampler, uv + Direction * 1.384615) * 0.316216;
    sum += texture(SourceSampler, uv - Direction * 1.384615) * 0.316216;
    sum += texture(SourceSampler, uv + Direction * 3.230769) * 0.070270;
    sum += texture(SourceSampler, uv - Direction * 3.230769) * 0.070270;
    outColor = sum;
}
