#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 Viewport;
uniform vec3 Tint;
uniform vec4 Material; // body opacity, refraction in pixels, shine, edge strength
uniform float Time;
in vec2 texCoord;
in vec2 faceUV;
out vec4 fragColor;
void main(){
    float mask=texture(Sampler0,texCoord).a;
    if(mask<0.1)discard;
    vec2 dx=dFdx(texCoord)*1.1,dy=dFdy(texCoord)*1.1;
    float neighbor=min(min(texture(Sampler0,texCoord+dx).a,texture(Sampler0,texCoord-dx).a),
        min(texture(Sampler0,texCoord+dy).a,texture(Sampler0,texCoord-dy).a));
    vec2 faceDistance=min(faceUV,1.0-faceUV)/max(fwidth(faceUV),vec2(0.00001));
    float distancePx=min(faceDistance.x,faceDistance.y);
    float edge=max(1.0-smoothstep(0.35,1.5,distancePx),1.0-neighbor);
    float halo=exp(-distancePx*.42);
    // Material highlights belong to the armor face, never to the camera/view direction.
    float rim=pow(clamp(1.0-16.0*faceUV.x*(1.0-faceUV.x)*faceUV.y*(1.0-faceUV.y),0.0,1.0),3.0);
    float wave=sin(faceUV.y*8.0+faceUV.x*3.0+Time*.7);
    float ripple=sin(faceUV.y*18.0-faceUV.x*5.0-Time)*.28;
    vec2 screenUV=gl_FragCoord.xy/Viewport;
    vec2 bend=vec2(wave*.8,ripple)*Material.y/Viewport;
    vec2 uv=clamp(screenUV+bend,1.0/Viewport,1.0-1.0/Viewport);
    vec3 refracted=texture(Sampler1,uv).rgb;
    float ribbon=pow(.5+.5*sin(faceUV.x*7.0+faceUV.y*1.5+wave*.45+Time*.35),18.0);
    float highlight=pow(.5+.5*sin(faceUV.x*11.0-faceUV.y*3.0-Time*.5),30.0);
    vec3 glass=refracted*mix(vec3(1.0),Tint,Material.x*.55);
    glass+=mix(Tint,vec3(1.0),.65)*(ribbon*.23+highlight*.35+rim*.18)*Material.z;
    glass+=mix(Tint,vec3(1.0),.8)*(edge*.8+halo*.13)*Material.w;
    float alpha=clamp(Material.x*.65+.2+(edge+halo*.3)*Material.w*.65+rim*.2,0.0,1.0);
    fragColor=vec4(glass,alpha);
}
