#version 150

in vec2 TexCoord;


uniform sampler2D Sampler0; 
uniform sampler2D Sampler1; 

uniform mat4 InvViewProj;
uniform float Ambient; 

uniform vec4 L0Pos; 
uniform vec4 L0Col; 
uniform vec4 L1Pos; 
uniform vec4 L1Col; 
uniform vec4 L2Pos; 
uniform vec4 L2Col; 
uniform vec4 L3Pos; 
uniform vec4 L3Col; 
uniform vec4 L4Pos; 
uniform vec4 L4Col; 
uniform vec4 L5Pos; 
uniform vec4 L5Col; 
uniform vec4 L6Pos; 
uniform vec4 L6Col; 
uniform vec4 L7Pos; 
uniform vec4 L7Col; 
uniform vec4 L8Pos; 
uniform vec4 L8Col; 
uniform vec4 L9Pos; 
uniform vec4 L9Col; 
uniform vec4 L10Pos; 
uniform vec4 L10Col; 
uniform vec4 L11Pos; 
uniform vec4 L11Col; 
uniform vec4 L12Pos; 
uniform vec4 L12Col; 
uniform vec4 L13Pos; 
uniform vec4 L13Col; 
uniform vec4 L14Pos; 
uniform vec4 L14Col; 
uniform vec4 L15Pos; 
uniform vec4 L15Col; 
uniform vec4 L16Pos; 
uniform vec4 L16Col; 
uniform vec4 L17Pos; 
uniform vec4 L17Col; 
uniform vec4 L18Pos; 
uniform vec4 L18Col; 
uniform vec4 L19Pos; 
uniform vec4 L19Col; 
uniform vec4 L20Pos; 
uniform vec4 L20Col; 
uniform vec4 L21Pos; 
uniform vec4 L21Col; 
uniform vec4 L22Pos; 
uniform vec4 L22Col; 
uniform vec4 L23Pos; 
uniform vec4 L23Col; 
uniform vec4 L24Pos; 
uniform vec4 L24Col; 
uniform vec4 L25Pos; 
uniform vec4 L25Col; 
uniform vec4 L26Pos; 
uniform vec4 L26Col; 
uniform vec4 L27Pos; 
uniform vec4 L27Col; 
uniform vec4 L28Pos; 
uniform vec4 L28Col; 
uniform vec4 L29Pos; 
uniform vec4 L29Col; 
uniform vec4 L30Pos; 
uniform vec4 L30Col; 
uniform vec4 L31Pos; 
uniform vec4 L31Col; 
uniform vec4 L32Pos; 
uniform vec4 L32Col; 
uniform vec4 L33Pos; 
uniform vec4 L33Col; 
uniform vec4 L34Pos; 
uniform vec4 L34Col; 
uniform vec4 L35Pos; 
uniform vec4 L35Col; 
uniform vec4 L36Pos; 
uniform vec4 L36Col; 
uniform vec4 L37Pos; 
uniform vec4 L37Col; 
uniform vec4 L38Pos; 
uniform vec4 L38Col; 
uniform vec4 L39Pos; 
uniform vec4 L39Col; 
uniform vec4 L40Pos; 
uniform vec4 L40Col; 
uniform vec4 L41Pos; 
uniform vec4 L41Col; 
uniform vec4 L42Pos; 
uniform vec4 L42Col; 
uniform vec4 L43Pos; 
uniform vec4 L43Col; 
uniform vec4 L44Pos; 
uniform vec4 L44Col; 
uniform vec4 L45Pos; 
uniform vec4 L45Col; 
uniform vec4 L46Pos; 
uniform vec4 L46Col; 
uniform vec4 L47Pos; 
uniform vec4 L47Col; 

out vec4 OutColor;



const float GAIN = 2.0;

const float BOOST = 1.7;

const float LIFT = 0.12;

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 pos = InvViewProj * clip;
    return pos.xyz / pos.w;
}


vec3 faceNormal(vec2 uv, float depth, vec3 world) {
    vec2 texel = 1.0 / vec2(textureSize(Sampler1, 0));

    vec2 uvL = uv - vec2(texel.x, 0.0);
    vec2 uvR = uv + vec2(texel.x, 0.0);
    vec2 uvD = uv - vec2(0.0, texel.y);
    vec2 uvU = uv + vec2(0.0, texel.y);

    float dL = texture(Sampler1, uvL).r;
    float dR = texture(Sampler1, uvR).r;
    float dD = texture(Sampler1, uvD).r;
    float dU = texture(Sampler1, uvU).r;

    vec3 ddx = abs(dL - depth) < abs(dR - depth)
        ? world - worldFromDepth(uvL, dL)
        : worldFromDepth(uvR, dR) - world;

    vec3 ddy = abs(dD - depth) < abs(dU - depth)
        ? world - worldFromDepth(uvD, dD)
        : worldFromDepth(uvU, dU) - world;

    vec3 n = cross(ddx, ddy);
    float len = length(n);
    
    if (len < 1e-8) return normalize(-world);

    n /= len;
    
    
    return dot(n, -world) < 0.0 ? -n : n;
}



vec3 contribution(vec3 world, vec3 normal, vec4 posRad, vec4 col) {
    float radius = posRad.w;
    if (radius <= 0.0001 || col.a <= 0.0001) return vec3(0.0);

    vec3 delta = posRad.xyz - world;
    float d = length(delta);
    if (d >= radius) return vec3(0.0);

    
    
    
    float falloff = smoothstep(0.0, 1.0, 1.0 - d / radius);

    float ndotl = d > 0.0001 ? dot(normal, delta / d) : 1.0;
    
    
    
    float lambert = clamp((ndotl + Ambient) / (1.0 + Ambient), 0.0, 1.0);

    return col.rgb * (col.a * falloff * lambert);
}

void main() {
    vec4 src = texture(Sampler0, TexCoord);

    float depth = texture(Sampler1, TexCoord).r;
    if (depth >= 1.0) {
        
        OutColor = src;
        return;
    }

    vec3 world = worldFromDepth(TexCoord, depth);
    vec3 normal = faceNormal(TexCoord, depth, world);

    vec3 sum = vec3(0.0);
    sum += contribution(world, normal, L0Pos, L0Col);
    sum += contribution(world, normal, L1Pos, L1Col);
    sum += contribution(world, normal, L2Pos, L2Col);
    sum += contribution(world, normal, L3Pos, L3Col);
    sum += contribution(world, normal, L4Pos, L4Col);
    sum += contribution(world, normal, L5Pos, L5Col);
    sum += contribution(world, normal, L6Pos, L6Col);
    sum += contribution(world, normal, L7Pos, L7Col);
    sum += contribution(world, normal, L8Pos, L8Col);
    sum += contribution(world, normal, L9Pos, L9Col);
    sum += contribution(world, normal, L10Pos, L10Col);
    sum += contribution(world, normal, L11Pos, L11Col);
    sum += contribution(world, normal, L12Pos, L12Col);
    sum += contribution(world, normal, L13Pos, L13Col);
    sum += contribution(world, normal, L14Pos, L14Col);
    sum += contribution(world, normal, L15Pos, L15Col);
    sum += contribution(world, normal, L16Pos, L16Col);
    sum += contribution(world, normal, L17Pos, L17Col);
    sum += contribution(world, normal, L18Pos, L18Col);
    sum += contribution(world, normal, L19Pos, L19Col);
    sum += contribution(world, normal, L20Pos, L20Col);
    sum += contribution(world, normal, L21Pos, L21Col);
    sum += contribution(world, normal, L22Pos, L22Col);
    sum += contribution(world, normal, L23Pos, L23Col);
    sum += contribution(world, normal, L24Pos, L24Col);
    sum += contribution(world, normal, L25Pos, L25Col);
    sum += contribution(world, normal, L26Pos, L26Col);
    sum += contribution(world, normal, L27Pos, L27Col);
    sum += contribution(world, normal, L28Pos, L28Col);
    sum += contribution(world, normal, L29Pos, L29Col);
    sum += contribution(world, normal, L30Pos, L30Col);
    sum += contribution(world, normal, L31Pos, L31Col);
    sum += contribution(world, normal, L32Pos, L32Col);
    sum += contribution(world, normal, L33Pos, L33Col);
    sum += contribution(world, normal, L34Pos, L34Col);
    sum += contribution(world, normal, L35Pos, L35Col);
    sum += contribution(world, normal, L36Pos, L36Col);
    sum += contribution(world, normal, L37Pos, L37Col);
    sum += contribution(world, normal, L38Pos, L38Col);
    sum += contribution(world, normal, L39Pos, L39Col);
    sum += contribution(world, normal, L40Pos, L40Col);
    sum += contribution(world, normal, L41Pos, L41Col);
    sum += contribution(world, normal, L42Pos, L42Col);
    sum += contribution(world, normal, L43Pos, L43Col);
    sum += contribution(world, normal, L44Pos, L44Col);
    sum += contribution(world, normal, L45Pos, L45Col);
    sum += contribution(world, normal, L46Pos, L46Col);
    sum += contribution(world, normal, L47Pos, L47Col);

    
    
    
    vec3 lightAmount = vec3(1.0) - exp(-sum * GAIN);

    
    
    
    vec3 lit = src.rgb * (vec3(1.0) + lightAmount * BOOST) + lightAmount * LIFT;

    OutColor = vec4(clamp(lit, 0.0, 1.0), src.a);
}
