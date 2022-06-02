# mybatis-crypto

使用注解方式实现Mybatis框架数据的加解密  
目前仅支持使用了TkMapper和MyBatis Plus框架的项目  
示例项目：[https://gitee.com/liujing_yanghui/mybatis-crypto-sample](https://gitee.com/liujing_yanghui/mybatis-crypto-sample)

## 安装
### 基于Tk MyBatis框架
#### maven
```
    <dependency>
      <groupId>top.liujingyanghui</groupId>
      <artifactId>tk-mybatis-crypto</artifactId>
      <version>last-version</version>
    </dependency>
```
#### gradle
```
    implementation 'top.liujingyanghui:tk-mybatis-crypto:last-version'
```

### 基于MyBatis Plus框架
#### maven
```
    <dependency>
      <groupId>top.liujingyanghui</groupId>
      <artifactId>mybatis-plus-crypto</artifactId>
      <version>last-version</version>
    </dependency>
```
#### gradle
```
    implementation 'top.liujingyanghui:mybatis-plus-crypto:last-version'
```

## 如何使用
#### 使用前注意事项
1. 所有加密的实体必须实现Serializable接口

#### 一、创建加解密规则
```java
/**
 * 手机号加解密
 *
 * @author : wdh
 * @since : 2022/5/23 16:15
 */
public class PhoneCryptoRule extends AbstractCryptoRule {

    private final String KEY = "0123456789ABHAEQ";
    private final AES AES = SecureUtil.aes(KEY.getBytes());

    /**
     * 数据加密
     */
    public String encrypt(String content) {
        try {
            return AES.encryptHex(content);
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 数据解密
     */
    public String decrypt(String content) {
        try {
            return AES.decryptStr(content);
        } catch (Exception e) {
            return content;
        }
    }
}
```

#### 二、实体类和属性加注解
在需要加解密的实体上加@CryptoClass和@CryptoString注解
```java
    @Data
    @CryptoClass    // 标识这个实体中有属性需要加解密
    @EqualsAndHashCode(callSuper = true)
    public class User extends BaseEntity { // BaseEntity中已经实现Serializable，这里就不需要实现了
    
        private String name;
    
        // 标识这个属性需要加解密，设置加解密的规则为手机号
        @CryptoString(rule = PhoneCryptoRule.class, mode = CryptoMode.ALL)
        private String phone;
    
        // 标识这个属性需要加解密，设置加解密的规则为身份证号
        @CryptoString(rule = IdNumCryptoRule.class, mode = CryptoMode.ALL)
        private String idNum;
    }
```
#### 三、Tk MyBatis框架加解密的使用
* 增
```java
    public void insert() {
        User user = new User();
        user.setPhone("13312345678");
        user.setIdNum("520520520520520520");
        user.setName("mybatis");
        userMapper.insert(user);
    }
```
* 查询与更新
```java
    public void queryAndUpdate() {
        // example 会根据实体的注解加密后去查询
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo("phone", "13333333333");
        // 查询出来的数据是根据注解解密后的数据
        User user = userMapper.selectOneByExample(example); 
        user.setPhone("13333333340");
        // 操作时手机号会根据注解自动加密
        userMapper.updateByPrimaryKey(user);
    }
```

#### 四、MyBatis Plus框架加解密的使用
为了能够让框架封装的Wrapper支持根据实体类和属性的注解进行相应的加解密  
需要使用 LambdaQueryWrapperX、LambdaUpdateWrapperX、QueryWrapperX、UpdateWrapperX 代替框架的相应Wrapper去操作  
同时提供了快速创建相应对象的静态方法，具体使用如下：
```java
     LambdaQueryWrapperX<User> wrapper = WrapperX.lambdaQuery(User.class);
     
     QueryWrapperX<User> wrapper = WrapperX.query(User.class);
     
     LambdaUpdateWrapperX<User> wrapper = WrapperX.lambdaUpdate(User.class);
     
     UpdateWrapperX<User> wrapper = WrapperX.update(User.class);
```
* 增
```java
    public void insert() {
        User user = new User();
        user.setPhone("13312345678");
        user.setIdNum("520520520520520520");
        user.setName("mybatis");
        userMapper.insert(user);
    }
```
* 查询与更新
```java
    public void queryAndUpdate() {
        // 新建一个 LambdaQueryWrapperX 这种方式支持根据实体注解去加密查询
        LambdaQueryWrapperX<User> wrapper = WrapperX.lambdaQuery(User.class); 
        wrapper.eq(User::getPhone,"13333333333");
        // 查询出来的数据为解密后数据
        User user = userMapper.selectOne(wrapper);
        user.setIdNum("520520520520520");
        // 更新时会根据注解去加密更新
        userMapper.updateById(user); 
    }
```

#### 五、mapper接口中方法加解密的使用
```java
public interface UserMapper extends Mapper<User> {

    // 入参和出参都为Map情况，标识入参和出参中key为phone的属性使用PhoneCryptoRule规则去加解密
    @CryptoKey(key = "phone", rule = PhoneCryptoRule.class)
    List<Map<String, Object>> selectByMap(Map<String, String> param);

    // 入参或出参为实体情况，需要去实体中使用@CryptoClass和@CryptoString注解
    List<User> selectByEntity(@Param("user") User user);

    User selectOneByEntity(User user);

    // 入参为单个List<String>、出参为List<String>情况，使用@CryptoString注解加解密
    @CryptoString(rule = PhoneCryptoRule.class)
    List<String> selectByPhones(List<String> phoneS);

    // 入参为单个String数组、出参为String[]使用@CryptoString注解加解密
    @CryptoString(rule = PhoneCryptoRule.class)
    String[] selectByPhoneArr(String[] phoneS);

    // 入参为单个String且没有@Param注解、出参为String时，使用@CryptoString注解加解密
    @CryptoStrings({
            @CryptoString(rule = PhoneCryptoRule.class, mode = CryptoMode.ENCRYPT),
            @CryptoString(rule = IdNumCryptoRule.class, mode = CryptoMode.DECRYPT)
    })
    String selectIdNumByPhone(String phone);

    // 入参为单个String且有@Param注解需使用@CryptoKey注解加密
    @CryptoString(rule = IdNumCryptoRule.class, mode = CryptoMode.DECRYPT)
    @CryptoKey(key = "phone", rule = PhoneCryptoRule.class, mode = CryptoMode.ENCRYPT)
    String selectIdNumByPhone2(@Param("phone") String phone);

    // 入参为多个String、出参为String时的情况
    @CryptoKeys({
            @CryptoKey(key = "phone", rule = PhoneCryptoRule.class, mode = CryptoMode.ENCRYPT),
            @CryptoKey(key = "idNum", rule = IdNumCryptoRule.class, mode = CryptoMode.ENCRYPT)
    })
    @CryptoString(rule = IdNumCryptoRule.class, mode = CryptoMode.DECRYPT)
    String selectIdNumByPhoneAndIdNum(@Param("phone") String phone, @Param("idNum") String idNum);

    @CryptoKeys({
            @CryptoKey(key = "phone", rule = PhoneCryptoRule.class, mode = CryptoMode.DECRYPT),
            @CryptoKey(key = "idNum", rule = IdNumCryptoRule.class, mode = CryptoMode.DECRYPT)
    })
    List<Map<String, Object>> selectByList(List<User> users);

    // 入参为多种情况下的示例
    List<User> selectByUserParam(UserParam param);
}
```
mapper接口方法参数为实体，实体中属性复杂情况说明
```java
@Data
@CryptoClass
public class UserParam implements Serializable {

    @CryptoClass   // 对象中包含实体，实体中有字段需要加密，属性需要加@CryptoClass
    private UserItem userItem;

    @CryptoString(rule = PhoneCryptoRule.class)  // 集合泛型为String的情况
    private List<String> phones;

    @CryptoClass // 集合泛型为实体时，实体中有字段需要加密，属性需要加@CryptoClass
    private List<UserItem> phoneList;

    @CryptoKey(key = "phone", rule = PhoneCryptoRule.class)  // 集合是Map的情况
    private List<Map<String, String>> phoneMap;

    @CryptoString(rule = PhoneCryptoRule.class) // 数组是String的情况
    private String[] phoneArray;

    @Data
    public static class UserItem implements Serializable{
        @CryptoString(rule = PhoneCryptoRule.class)
        private String phone;
    }
}
```

## 注解说明
#### @CryptoClass
    使用说明：标识为该类下有敏感信息，标识后才会继续扫描实体类中属性的注解
    使用位置：实体类、实体类中的属性类型为实体对象

#### @CryptoString
    使用说明：标识String类型（包括String的集合和数组）的属性、方法入参（只有一个参数并且没有@Param注解）、方法出参需要加解密
    使用位置：实体属性、mapper接口方法
##### 属性
| 属性   | 类型                           | 是否必须 | 默认值                      | 描述    |
|------|------------------------------|------|--------------------------|-------|
| rule | Class<? extends ICryptoRule> | 是    | AbstractCryptoRule.class | 加解密规则 |
| mode | CryptoMode                   | 否    | CryptoMode.ALL           | 加解密模式 |

#### @CryptoKey
    使用说明：标识Map对象中的key值需要加解密
    使用位置：实体属性、mapper接口方法
##### 属性
| 属性   | 类型                           | 是否必须 | 默认值                      | 描述      |
|------|------------------------------|------|--------------------------|---------|
| key  | String                       | 是    | ""                       | 加解密key值 |
| rule | Class<? extends ICryptoRule> | 是    | AbstractCryptoRule.class | 加解密规则   |
| mode | CryptoMode                   | 否    | CryptoMode.ALL           | 加解密模式   |

#### @CryptoStrings
    使用说明：标识有多个@CryptoString注解
    使用位置：mapper接口方法
##### 属性
| 属性    | 类型             | 是否必须 | 默认值 | 描述                 |
|-------|----------------|------|-----|--------------------|
| value | CryptoString[] | 是    | {}  | 多个加解密@CryptoString |

#### @CryptoKeys
    使用说明：标识Map对象中含有多个key值需要加解密
    使用位置：实体属性、mapper接口方法
##### 属性
| 属性    | 类型          | 是否必须 | 默认值 | 描述              |
|-------|-------------|------|-----|-----------------|
| value | CryptoKey[] | 是    | {}  | 多个加解密@CryptoKey |

## 捐赠
如果它给你带来了帮助，可以请开源者吃包辣条喝杯奶茶哟！在此表示感谢^_^
![微信](wx.png)
![微信](zfb.png)
