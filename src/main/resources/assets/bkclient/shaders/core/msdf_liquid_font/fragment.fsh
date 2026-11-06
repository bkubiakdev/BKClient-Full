#version 150

in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float Range;
uniform float Thickness;
uniform float Smoothness;
uniform vec2 ScreenSize;
uniform float DistortStrength;
uniform float TintStrength;
uniform float GlobalAlpha;
uniform vec4 ColorModulator;

out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    vec4 texColor = texture(Sampler0, TexCoord);
    float dist = median(texColor.rgb) - 0.5 + Thickness;
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * textureSize(Sampler0, 0);
    float pixels = Range * inversesqrt(h.x * h.x + h.y * h.y);
    float signedPixels = dist * pixels;
    float alpha = smoothstep(-Smoothness, Smoothness, signedPixels);

    vec2 grad = vec2(dFdx(signedPixels), dFdy(signedPixels));
    float gradLen = length(grad);
    vec2 normal = gradLen > 0.01 ? normalize(grad) : vec2(0.0, -1.0);
    normal = clamp(normal, -1.0, 1.0);

    float body = smoothstep(-2.0, 2.0, signedPixels);
    float edgeFade = smoothstep(0.0, 4.0, abs(signedPixels));
    float strength = body * edgeFade * DistortStrength;

    vec2 uv = GlobalPos;
    vec2 refractUv = uv + normal * strength * 0.5;

    vec3 refracted = texture(Sampler1, refractUv).rgb;

    vec3 tinted = mix(refracted, FragColor.rgb, TintStrength * 0.4);

    OutColor = vec4(tinted, alpha * GlobalAlpha) * ColorModulator;

    if (OutColor.a < 0.01) {
        discard;
    }
}
