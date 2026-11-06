#version 150
uniform sampler2D Sampler0;
uniform float Range;
uniform float OutlineWidth;
uniform vec4 OutlineColor;
uniform float Softness;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
float median(vec3 c) { return max(min(c.r,c.g),min(max(c.r,c.g),c.b)); }
void main() {
    vec2 unitRange=vec2(Range)/vec2(textureSize(Sampler0,0));
    float pixels=max(0.5*dot(unitRange,1.0/max(fwidth(texCoord),vec2(0.000001))),1.0);
    float distance=(median(texture(Sampler0,texCoord).rgb)-0.5)*pixels;
    float fill=smoothstep(-Softness,Softness,distance);
    float edge=smoothstep(-Softness,Softness,distance+OutlineWidth);
    float outline=max(0.0,edge-fill)*OutlineColor.a;
    float coverage=fill+outline;
    vec3 rgb=(vertexColor.rgb*fill+OutlineColor.rgb*outline)/max(coverage,0.00001);
    fragColor=vec4(rgb,coverage*vertexColor.a);
    if(fragColor.a<0.003)discard;
}
