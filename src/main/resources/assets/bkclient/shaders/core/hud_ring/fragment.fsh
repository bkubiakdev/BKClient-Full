#version 150
in vec2 uv;
in vec4 color;
uniform float Extent, Radius, HalfWidth, Start, Sweep;
uniform int Rounded;
out vec4 fragColor;
const float TAU=6.28318530718;
vec2 point(float a){return vec2(sin(a),-cos(a))*Radius;}
void main(){
 vec2 p=(uv*2.0-1.0)*Extent;
 float angle=mod(atan(p.x,-p.y)-Start+TAU*4.0,TAU);
 float d=abs(length(p)-Radius)-HalfWidth;
 if(Sweep<TAU-0.0001 && angle>Sweep){
   float endpoint=min(length(p-point(Start)),length(p-point(Start+Sweep)));
   d=Rounded==1 ? endpoint-HalfWidth : max(d, min(angle-Sweep,TAU-angle)*Radius);
 }
 float aa=max(length(vec2(dFdx(d),dFdy(d)))*0.5,0.0001);
 float coverage=1.0-smoothstep(-aa,aa,d);
 fragColor=vec4(color.rgb,color.a*coverage);
}
