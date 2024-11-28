package com.gyso.ndklearnapplication;

import android.util.Log;

import java.util.Arrays;

public class H264SpsParser {
    private final static String TAG = H264SpsParser.class.getCanonicalName();
    private final static int NAL_HEADER = 0;

    private static int nStartBit = 0;


    private static int Ue(byte[] pBuff, int nLen) {
        int nZeroNum = 0;
        while (nStartBit < nLen * 8) {
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }
        nStartBit++;

        int dwRet = 0;
        for (int i = 0; i < nZeroNum; i++) {
            dwRet <<= 1;
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                dwRet += 1;
            }
            nStartBit++;
        }
        return (1 << nZeroNum) - 1 + dwRet;
    }

    private static int Se(byte[] pBuff, int nLen) {
        int UeVal = Ue(pBuff, nLen);
        double k = UeVal;
        int nValue = (int) Math.ceil(k / 2);
        if (UeVal % 2 == 0) {
            nValue = -nValue;
        }
        return nValue;
    }

    private static int u(int BitCount, byte[] buf) {
        int dwRet = 0;
        for (int i = 0; i < BitCount; i++) {
            dwRet <<= 1;
            if ((buf[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                dwRet += 1;
            }
            nStartBit++;
        }
        return dwRet;
    }

    public static boolean h264_decode_seq_parameter_set(byte[] buf, int nLen, int[] size) {
        nStartBit = 0;
        int forbidden_zero_bit = u(1, buf);
        int nal_ref_idc = u(2, buf);
        int nal_unit_type = u(5, buf);
        if (nal_unit_type == 7) {
            int profile_idc = u(8, buf);
            int constraint_set0_flag = u(1, buf);//(buf[1] & 0x80)>>7;
            int constraint_set1_flag = u(1, buf);//(buf[1] & 0x40)>>6;
            int constraint_set2_flag = u(1, buf);//(buf[1] & 0x20)>>5;
            int constraint_set3_flag = u(1, buf);//(buf[1] & 0x10)>>4;
            int reserved_zero_4bits = u(4, buf);
            int level_idc = u(8, buf);

            int seq_parameter_set_id = Ue(buf, nLen);

            if (profile_idc == 100 || profile_idc == 110 ||
                    profile_idc == 122 || profile_idc == 144) {
                int chroma_format_idc = Ue(buf, nLen);
                if (chroma_format_idc == 3) {
                    int residual_colour_transform_flag = u(1, buf);
                }
                int bit_depth_luma_minus8 = Ue(buf, nLen);
                int bit_depth_chroma_minus8 = Ue(buf, nLen);
                int qpprime_y_zero_transform_bypass_flag = u(1, buf);
                int seq_scaling_matrix_present_flag = u(1, buf);

                int[] seq_scaling_list_present_flag = new int[8];
                if (seq_scaling_matrix_present_flag != 0) {
                    for (int i = 0; i < 8; i++) {
                        seq_scaling_list_present_flag[i] = u(1, buf);
                    }
                }
            }
            int log2_max_frame_num_minus4 = Ue(buf, nLen);
            int pic_order_cnt_type = Ue(buf, nLen);
            if (pic_order_cnt_type == 0) {
                int log2_max_pic_order_cnt_lsb_minus4 = Ue(buf, nLen);
            } else if (pic_order_cnt_type == 1) {
                int delta_pic_order_always_zero_flag = u(1, buf);
                int offset_for_non_ref_pic = Se(buf, nLen);
                int offset_for_top_to_bottom_field = Se(buf, nLen);
                int num_ref_frames_in_pic_order_cnt_cycle = Ue(buf, nLen);

                int[] offset_for_ref_frame = new int[num_ref_frames_in_pic_order_cnt_cycle];
                for (int i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++) {
                    offset_for_ref_frame[i] = Se(buf, nLen);
                }
            }
            int num_ref_frames = Ue(buf, nLen);
            int gaps_in_frame_num_value_allowed_flag = u(1, buf);
            int pic_width_in_mbs_minus1 = Ue(buf, nLen);
            int pic_height_in_map_units_minus1 = Ue(buf, nLen);

            size[0] = (pic_width_in_mbs_minus1 + 1) * 16;
            size[1] = (pic_height_in_map_units_minus1 + 1) * 16;

            return true;
        }
        return false;
    }

    /**
     *             byte[] noHeaderSps = new byte[sps.length-4];
     *             System.arraycopy(sps, 4, noHeaderSps, 0, noHeaderSps.length);
     *             byte[] noHeaderPps = new byte[pps.length-4];
     *             System.arraycopy(pps, 4, noHeaderPps, 0, noHeaderPps.length);
     *             decodeSPS(noHeaderSps,noHeaderPps);
     * @param sps
     * @param pps
     */
    private void decodeSPS(byte[] sps, byte[] pps){
        Log.i(TAG, "decodeSPS: sps="+ Arrays.toString(sps));
        Log.i(TAG, "decodeSPS: pps="+Arrays.toString(pps));
        int i,offset = 32;
        int pic_width_len,pic_height_len;
        int profile_idc = sps[1];
        byte[] header_pps = new byte[pps.length];
        byte[] header_sps = new byte[sps.length];
        System.arraycopy(sps,0,header_sps,0,sps.length);
        System.arraycopy(pps,0,header_pps,0,pps.length);
        offset += getUeLen(sps,offset);//jump seq_parameter_set_id
        if(profile_idc == 100 || profile_idc == 110 || profile_idc == 122
                || profile_idc == 144) {
            int chroma_format_idc = (getUeLen(sps,offset) == 1)?0:
                    ( sps[(offset+getUeLen(sps,offset))/8] >>
                            (7-((offset+getUeLen(sps,offset))%8)) );
            offset += getUeLen(sps,offset);//jump chroma_format_idc
            if(chroma_format_idc == 3)
                offset++; //jump residual_colour_transform_flag
            offset += getUeLen(sps,offset);//jump bit_depth_luma_minus8
            offset += getUeLen(sps,offset);//jump bit_depth_chroma_minus8
            offset ++; //jump qpprime_y_zero_transform_bypass_flag
            int seq_scaling_matrix_present_flag = (sps[offset/8] >> (8-(offset%8)))&0x01;
            if(seq_scaling_matrix_present_flag == 1) offset += 8; //jump seq_scaling_list_present_flag
        }
        offset += getUeLen(sps,offset);//jump log2_max_frame_num_minus4
        int pic_order_cnt_type = (getUeLen(sps,offset) == 1)?0:
                ( sps[(offset+getUeLen(sps,offset))/8] >>
                        (7-((offset+getUeLen(sps,offset))%8)) );
        offset += getUeLen(sps,offset);
        if(pic_order_cnt_type == 0) {
            offset += getUeLen(sps,offset);
        }
        else if(pic_order_cnt_type == 1) {
            offset++; //jump delta_pic_order_always_zero_flag
            offset += getUeLen(sps,offset); //jump offset_for_non_ref_pic
            offset += getUeLen(sps,offset); //jump offset_for_top_to_bottom_field
            int num_ref_frames_inpic_order_cnt_cycle = ( sps[(offset+getUeLen(sps,offset))/8] >>
                    (7-((offset+getUeLen(sps,offset))%8)) );
            for(i=0; i<num_ref_frames_inpic_order_cnt_cycle; ++i)
                offset += getUeLen(sps,offset); //jump ref_frames_inpic_order
        }
        offset += getUeLen(sps,offset); // jump num_ref_frames
        offset++; // jump gaps_in_fram_num_value_allowed_flag

        pic_width_len = getUeLen(sps,offset);
        int picWidth = (getByteBit(sps,offset+pic_width_len/2,pic_width_len/2+1)+1)*16;
        offset += pic_width_len;
        pic_height_len = getUeLen(sps,offset);
        int picHeight = (getByteBit(sps,offset+pic_height_len/2,pic_height_len/2+1)+1)*16;
        Log.e(TAG, "The picWidth = " + picWidth + " ,the picHeight = " + picHeight);
    }

    private int getUeLen(byte[] bytes, int offset) {
        int zcount = 0;
        while(true) {
            if(( ( bytes[offset/8] >> (7-(offset%8)) ) & 0x01 ) == 0) {
                offset ++;
                zcount ++;
            }
            else break;
        }
        return zcount * 2 + 1;
    }

    /*
     * This method is get the bit[] from a byte[]
     * It may have a more efficient way
     */
    /*
     * This method is get the bit[] from a byte[]
     * It may have a more efficient way
     */
    public int getByteBit(byte[] bytes, int offset, int len){
        int tmplen = len/8+ ((len%8+offset%8)>8?1:0) + ((offset%8 == 0)?0:1);
        int lastByteZeroNum = ((len%8+offset%8-8)>0)?(16-len%8-offset%8):(8-len%8-offset%8);
        int data = 0;
        byte tmpC = (byte) (0xFF >> (8 - lastByteZeroNum));
        byte[] tmpB = new byte[tmplen];
        byte[] tmpA = new byte[tmplen];
        int i;
        for(i = 0;i<tmplen;++i) {
            if(i == 0) tmpB[i] = (byte) (bytes[offset/8] << (offset%8) >> (offset%8));
            else if(i+1 == tmplen) tmpB[i] = (byte) ((bytes[offset/8+i] & 0xFF) >> lastByteZeroNum);
            else tmpB[i] = bytes[offset/8+i];
            tmpA[i] = (byte) ((tmpB[i] & tmpC)<<(8-lastByteZeroNum));
            if(i+1 != tmplen && i != 0) {
                tmpB[i] = (byte) ((tmpB[i]&0xFF) >> lastByteZeroNum);
                tmpB[i] = (byte) (tmpB[i] | tmpA[i-1]);
            }
            else if(i == 0) tmpB[0] = (byte) ((tmpB[0]&0xFF) >> lastByteZeroNum);
            else tmpB[i] = (byte) (tmpB[i] | tmpA[i-1]);
            data = ((tmpB[i]&0xFF) << ((tmplen-i-1)*8)) | data ;
        }
        return data;
    }
}