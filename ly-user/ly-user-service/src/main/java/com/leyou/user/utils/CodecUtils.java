package com.leyou.user.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
//import org.springframework.util.DigestUtils;
import java.util.UUID;

public class CodecUtils {



    public static String md5Hex(String data,String salt) {
        if (StringUtils.isBlank(salt)) {
            salt = data.hashCode() + "";
        }
        return DigestUtils.md5Hex(salt + DigestUtils.md5Hex(data));
    }

    public static String shaHex(String data, String salt) {
        if (StringUtils.isBlank(salt)) {
            salt = data.hashCode() + "";
        }
        return DigestUtils.sha512Hex(salt + DigestUtils.sha512Hex(data));
    }

    public static String generateSalt(){
        return StringUtils.replace(UUID.randomUUID().toString(), "-", "");
    }

    public static void main(String[] args) {
        String salt = generateSalt();
        String code1 = md5Hex("ZHANG6498372", salt);
        System.out.println("salt = " + salt);
        System.out.println("加密后 = " + code1);
        System.out.println("-------------------");

        String code2 = shaHex(code1, salt);
        System.out.println("解密后 = " + code2);
    }
}
