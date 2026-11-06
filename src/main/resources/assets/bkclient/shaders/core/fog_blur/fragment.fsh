#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec4 FogParams;
uniform float BlurStrength;
uniform float BlurFloor;
uniform float SkipSky;

out vec4 OutColor;

float linearizeDepth(float depth, float near, float far) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - z * (far - near));
}

void main() {
    float depth = texture(Sampler2, TexCoord).r;
    vec3 sharp = texture(Sampler0, TexCoord).rgb;

    if (SkipSky > 0.5 && depth >= 0.99999) {
        OutColor = vec4(sharp, 1.0) * FragColor;
        return;
    }

    float linZ = linearizeDepth(depth, FogParams.z, FogParams.w);

    float t = smoothstep(FogParams.x, FogParams.y, linZ) * BlurStrength;
    t = max(t, BlurFloor);
    t = clamp(t, 0.0, 1.0);

    vec3 blurred = texture(Sampler1, TexCoord).rgb;

    OutColor = vec4(mix(sharp, blurred, t), 1.0) * FragColor;
}
