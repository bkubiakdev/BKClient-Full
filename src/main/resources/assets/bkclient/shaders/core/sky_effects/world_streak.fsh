#version 150
uniform float Part;
uniform float SquareHead;
uniform float Brightness;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec3 light;
    if(Part<.5) {
        float along=clamp(texCoord.x,0.,1.);
        float width=mix(.025,.20,along*along);
        float aa=max(fwidth(texCoord.y)*.65,.01);
        float core=exp(-pow(texCoord.y/max(width,aa),2.))*width/max(width,aa);
        float halo=exp(-texCoord.y*texCoord.y/2.2)*.13+exp(-texCoord.y*texCoord.y/.28)*.24;
        float fade=pow(along,1.5);
        light=(mix(vertexColor.rgb,vec3(1.),.6)*core*1.1+vertexColor.rgb*halo)*fade;
    } else if(Part<1.5) {
        float d=length(texCoord);
        float aa=max(fwidth(d)*.65,.01);
        float core=1.-smoothstep(.7-aa,1.+aa,d);
        float halo=exp(-dot(texCoord,texCoord)/3.5)*.38+exp(-dot(texCoord,texCoord)/14.)*.13;
        light=mix(vertexColor.rgb,vec3(1.),.8)*core*(1.-SquareHead)+vertexColor.rgb*halo;
    } else {
        float edge=max(abs(texCoord.x),abs(texCoord.y));
        float aa=max(fwidth(edge),.02);
        float rim=smoothstep(.72-aa,.94,edge);
        float fill=.32+.18*(1.-texCoord.y)*.5;
        light=vertexColor.rgb*fill+mix(vertexColor.rgb,vec3(1.),.85)*rim*.85;
    }
    fragColor=vec4(clamp(light*Brightness,0.,1.),vertexColor.a);
}
