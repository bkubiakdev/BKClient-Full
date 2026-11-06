#version 150

#moj_import <bkclient:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float alpha;
    if (Smoothness < 0.0) {
        float distance = roundedBoxSDF(center - (FragCoord * Size), center + Smoothness, Radius);
        float halfPixel = max(fwidth(distance) * 0.5, 0.0001);
        alpha = 1.0 - smoothstep(-halfPixel, halfPixel, distance);
    } else {
        float distance = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);
        alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);
    }
    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}
