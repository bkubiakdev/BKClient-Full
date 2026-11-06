#version 150

in vec2 TexCoord;


uniform sampler2D Sampler0;


uniform float IsolationStrength; 
uniform vec3 IsolationColor;     
uniform float HueTolerance;      
uniform float MinSat;            
uniform float MinVal;            
uniform float BackgroundSat;     

out vec4 OutColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

void main() {
    vec4 col = texture(Sampler0, TexCoord);
    float l = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));

    float sat = 1.0;

    if (IsolationStrength > 0.001) {
        vec3 hsv = rgb2hsv(col.rgb);
        float targetHue = rgb2hsv(IsolationColor).x;

        
        float hd = abs(hsv.x - targetHue);
        hd = min(hd, 1.0 - hd);

        
        float hueMatch = 1.0 - smoothstep(HueTolerance, HueTolerance + 0.04, hd);
        float satMatch = smoothstep(MinSat, min(MinSat + 0.10, 1.0), hsv.y);
        float valMatch = smoothstep(MinVal, min(MinVal + 0.10, 1.0), hsv.z);

        float keep = hueMatch * satMatch * valMatch * IsolationStrength;

        
        sat = mix(BackgroundSat, 1.0, keep);
    }

    vec3 mixed = mix(vec3(l), col.rgb, sat);
    OutColor = vec4(mixed, col.a);
}
