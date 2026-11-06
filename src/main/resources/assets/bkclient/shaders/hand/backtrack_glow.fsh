#version 330 core
in vec2 uv;
out vec4 outColor;
uniform sampler2D ColorTexture;
uniform float time;
uniform vec3 glowColor;
void main() {
    vec4 original = texture(ColorTexture, uv);
    vec2 texel = 1.0 / vec2(textureSize(ColorTexture, 0));
    float halo = 0.0;
    float edge = 0.0;
    // 24 taps instead of a full 11x11 neighborhood; outer rings make the bloom visible.
    for (int i=0;i<8;i++) {
        float a=float(i)*0.78539816339;
        vec2 ray=vec2(cos(a),sin(a))*texel;
        float nearAlpha=texture(ColorTexture,uv+ray*2.0).a;
        float midAlpha=texture(ColorTexture,uv+ray*5.0).a;
        float farAlpha=texture(ColorTexture,uv+ray*9.0).a;
        halo += nearAlpha*0.5+midAlpha*0.32+farAlpha*0.18;
        edge += max(0.0,original.a-nearAlpha);
    }
    halo=clamp(halo/4.0,0.0,0.75);
    edge=clamp(edge,0.0,1.0);
    float pulse=0.92+sin(time*2.0)*0.08;
    float alpha=original.a > 0.005 ? edge * 0.55 : halo;
    if(alpha<0.002) discard;
    vec3 skin=original.rgb/max(original.a,0.001);
    float body=step(0.005,original.a);
    vec3 color=glowColor*1.3*pulse;
    outColor=vec4(color,alpha);
}
