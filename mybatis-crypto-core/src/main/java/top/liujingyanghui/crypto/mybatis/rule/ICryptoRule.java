package top.liujingyanghui.crypto.mybatis.rule;

/**
 * 加解密规则
 *
 * @author : wdh
 * @since : 2022/5/23 13:45
 */
public interface ICryptoRule {

    /**
     * 加密
     *
     * @param content 需要加密内容
     * @return 加密后内容
     */
    String encrypt(String content);

    /**
     * 解密
     *
     * @param content 需要解密内容
     * @return 解密后内容
     */
    String decrypt(String content);
}
