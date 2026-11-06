#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 starsPosition;
out vec3 starsNormal;
uniform vec3 StarsOrigin;

uniform vec4 WaveData;
uniform vec3 CameraOrigin;
vec3 bk_raw_wave(vec3 p, float wind) {
    float magnitude = sin(wind * 0.0027 + p.x + p.y) * WaveData.w + 0.04;
    float d0 = sin(wind * 0.0127);
    float d1 = sin(wind * 0.0089);
    float d2 = sin(wind * 0.0114);
    vec3 wave;
    wave.x = magnitude * sin(wind * 0.0224 + d1 + d2 + p.x - p.z + p.y);
    wave.y = magnitude * sin(wind * 0.0015 + d2 + d0 + p.x);
    wave.z = magnitude * sin(wind * 0.0063 + d0 + d1 - p.x + p.z + p.y);
    return wave;
}

void main() {
    vec3 pos = Position + ModelOffset;
    float alpha = Color.a;
    int tag = int(round(alpha * 255.0));

    if (tag >= 252 && tag <= 254) {
        vec3 worldPos = mod(Position + ModelOffset + CameraOrigin, 50.26548245743669);
        float wind = WaveData.x * 170.0;

        vec3 wave;
        if (tag == 254) {
            float sky = clamp(float(UV2.y) / 240.0, 0.0, 1.0);
            float gate = clamp(sky - 0.87, 0.0, 0.1);
            vec3 p = vec3(worldPos.x, worldPos.y * 0.5, worldPos.z);
            wave = bk_raw_wave(p, wind);
            wave.z = wave.z * 8.0 + wave.y * 4.0;
            wave.x *= 3.0;
            wave.y = 0.0;
            wave *= gate * WaveData.y;
        } else {
            vec3 p = worldPos * vec3(0.75, 0.375, 0.75);
            wave = bk_raw_wave(p, wind) * vec3(4.0, 3.0, 8.0);
            wave *= 0.1 * WaveData.z;
            if (tag == 252) {
                wave *= 0.75;
            }
        }

        pos += wave;
        alpha = 1.0;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = vec4(Color.rgb, alpha) * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
    starsPosition = Position + ModelOffset + StarsOrigin;
    starsNormal = Normal;
}
