#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

in vec3 starsPosition;
in vec3 starsNormal;
uniform vec4 StarsParams; // time, brightness, size, density
uniform vec3 StarsTint;
float starHash(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}
float filteredStar(vec2 d, vec2 dx, vec2 dy, vec2 radius) {
    vec2 variance=radius*radius;
    float a=variance.x+(dx.x*dx.x+dy.x*dy.x)/12.0;
    float b=(dx.x*dx.y+dy.x*dy.y)/12.0;
    float c=variance.y+(dx.y*dx.y+dy.y*dy.y)/12.0;
    float determinant=max(a*c-b*b,1e-12);
    float exponent=(c*d.x*d.x-2.0*b*d.x*d.y+a*d.y*d.y)/determinant;
    return sqrt(variance.x*variance.y/determinant)*exp(-0.5*max(exponent,0.0));
}
float surfaceStars() {
    vec3 n = abs(starsNormal);
    vec2 uv;
    float plane;
    if (n.y >= n.x && n.y >= n.z) { uv=starsPosition.xz; plane=starsPosition.y; }
    else if(n.x >= n.z) { uv=starsPosition.zy; plane=starsPosition.x; }
    else { uv=starsPosition.xy; plane=starsPosition.z; }
    uv *= StarsParams.w;
    vec2 dx=dFdx(uv), dy=dFdy(uv);
    float lod=1.0-smoothstep(0.12,0.5,max(length(dx),length(dy)));
    vec2 cell=mod(floor(uv),256.0);
    float seed=starHash(vec3(cell, mod(floor(plane*16.0+0.01),4096.0)));
    if(seed < 0.45) return 0.0;
    vec2 center=vec2(starHash(vec3(cell,seed*37.0)),starHash(vec3(cell,seed*91.0)))*0.5+0.25;
    vec2 d=(fract(uv)-center)/StarsParams.z;
    dx/=StarsParams.z; dy/=StarsParams.z;
    float core=filteredStar(d,dx,dy,vec2(0.011));
    float halo=filteredStar(d,dx,dy,vec2(0.056))*0.12;
    float rays=filteredStar(d,dx,dy,vec2(0.008,0.045))+filteredStar(d,dx,dy,vec2(0.045,0.008));
    float twinkle=0.2+0.8*pow(0.5+0.5*sin(StarsParams.x*(1.2+seed)+seed*113.0),2.0);
    return (core+halo+rays*0.32)*twinkle*lod*(1.0-smoothstep(24.0,64.0,vertexDistance));
}
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    if(StarsParams.y > 0.0) color.rgb += StarsTint * surfaceStars() * StarsParams.y;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
