uniform vec4 bkWaveData;
uniform vec3 bkCameraOrigin;
uniform vec3 bkStarsOrigin;
out vec3 bkStarsPosition;
out float bkDistance;
vec3 bk_raw_wave(vec3 p, float wind) {
    float magnitude = sin(wind * 0.0027 + p.x + p.y) * bkWaveData.w + 0.04;
    float d0 = sin(wind * 0.0127);
    float d1 = sin(wind * 0.0089);
    float d2 = sin(wind * 0.0114);
    vec3 wave;
    wave.x = magnitude * sin(wind * 0.0224 + d1 + d2 + p.x - p.z + p.y);
    wave.y = magnitude * sin(wind * 0.0015 + d2 + d0 + p.x);
    wave.z = magnitude * sin(wind * 0.0063 + d0 + d1 - p.x + p.z + p.y);
    return wave;
}


vec3 bkWind(vec3 position, uint tag, float skyLight) {
    if(tag == 0u || (bkWaveData.y <= 0.0 && bkWaveData.z <= 0.0)) return vec3(0.0);
    vec3 worldPos=mod(position+bkCameraOrigin,50.26548245743669);
    float wind=bkWaveData.x*170.0;
    vec3 wave;
    if(tag==1u) {
        // Sodium stores light texel centers in units of 1/256, vanilla uses 0..240.
        float sky=clamp((skyLight*256.0-8.0)/240.0,0.0,1.0);
        float gate=clamp(sky-0.87,0.0,0.1);
        wave=bk_raw_wave(vec3(worldPos.x,worldPos.y*.5,worldPos.z),wind);
        wave.z=wave.z*8.0+wave.y*4.0;wave.x*=3.0;wave.y=0.0;
        wave*=gate*bkWaveData.y;
    } else {
        wave=bk_raw_wave(worldPos*vec3(.75,.375,.75),wind)*vec3(4.0,3.0,8.0);
        wave*=.1*bkWaveData.z;
        if(tag==3u)wave*=.75;
    }
    return wave;
}
