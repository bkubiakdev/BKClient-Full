#version 150
in vec2 TexCoord;
uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform float FilterFog;
out vec4 OutColor;
void main() {
    vec4 fog = texture(Sampler2, TexCoord);
    if (FilterFog > 0.5) {
        vec2 d = 0.5 / vec2(textureSize(Sampler2, 0));
        fog = 0.25 * (texture(Sampler2, TexCoord + d) + texture(Sampler2, TexCoord - d)
           + texture(Sampler2, TexCoord + vec2(d.x,-d.y)) + texture(Sampler2, TexCoord + vec2(-d.x,d.y)));
    }
    vec4 scene = texture(Sampler0, TexCoord);
    OutColor = vec4(fog.rgb + scene.rgb * (1.0-fog.a), scene.a);
}
