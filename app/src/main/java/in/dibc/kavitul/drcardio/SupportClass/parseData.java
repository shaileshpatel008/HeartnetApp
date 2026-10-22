package in.dibc.kavitul.drcardio.SupportClass;

import in.dibc.kavitul.drcardio.Model.ecgData;

public class parseData {
    public void parseData() {

    }

    public static float[] charArrayToChannels(char[] data, Boolean testMode) {
        float[] channels = new float[12];
        for (int i = 0; i < 8; i++) {
            if (ecgData.hwVersion == 2) {
                channels[i] = joinbytes(data[3 * i], data[3 * i + 1], data[3 * i + 2], testMode);
            } else {
                channels[i] = joinMSBLSB(data[2 * i], data[2 * i + 1], testMode);
            }
        }
        generateAuxChannels(channels);
        return channels;
    }

    public static float[] generateAuxChannels(float[] channels) {
        channels[8] = (float) (channels[2] - channels[1]);
        channels[9] = (float) ((-1) * (channels[1] + channels[2]) / 2.0);
        channels[10] = (float) (channels[1] - (channels[2] / 2.0));
        channels[11] = (float) (channels[2] - (channels[1] / 2.0));
        return channels;
    }

    public static float joinbytes(char b1, char b2, char b3, Boolean testMode) {
        float joined = (float) ((b1 << 24) | (b2 << 16) | (b3 << 8));
        return joined / 256;
    }

    public static short joinMSBLSB(char MSB, char LSB, Boolean testMode) {
        short joined = (short) (((MSB << 8) | LSB));
//        if(settings.debug_mode){
//            joined = (short) (( (MSB<<8) | LSB ) );
//        }else{
//            joined = (short) (( (MSB<<8) | LSB ) <<1 );
//            /* Software gain of x2 */
//            /* KN: Removed software gain after discussion with VS. 1mv will now correspond to 54.61 in raw data value */
//        }
        return joined;
    }

    private boolean[] byteArrayToLeadStatus(byte[] statusBytes){
        boolean[] leadStatus = new boolean[10];
        leadStatus[0] = ( (statusBytes[0] & 0x08) == 0); /* Lead Status for V1 */
        leadStatus[3] = ( (statusBytes[0] & 0x01) == 0); /* Lead Status for V4 */
        leadStatus[2] = ( (statusBytes[0] & 0x02) == 0); /* Lead Status for V3 */
        leadStatus[1] = ( (statusBytes[1] & 0x80) == 0); /* Lead Status for V2 */
        leadStatus[4] = ( (statusBytes[0] & 0x04) == 0); /* Lead Status for V5 */
        leadStatus[5] = ( (statusBytes[1] & 0x10) == 0); /* Lead Status for V6 */
        leadStatus[6] = ( (statusBytes[1] & 0x40) == 0); /* Lead Status for Left Leg */
        leadStatus[7] = ( (statusBytes[0] & 0x08) == 0); /* Lead Status for Right Leg */
        leadStatus[8] = ( (statusBytes[1] & 0x20) == 0); /* Lead Status for Left Arm */
        leadStatus[9] = ( (statusBytes[2] & 0x20) == 0); /* Lead Status for Right Arm */
        return leadStatus;
    }
    private int byteArrayToBattLevel(byte[] statusBytes) {
        int battLevel = (statusBytes[2] & 0x0F)*7;
        // TODO: 17-09-2020 check with BARC team for logic to multiply with 7. ALso check if taking avg of 3 is necessory.
        return battLevel;
    }

}
