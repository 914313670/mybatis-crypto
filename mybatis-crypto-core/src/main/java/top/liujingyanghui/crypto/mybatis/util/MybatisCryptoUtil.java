package top.liujingyanghui.crypto.mybatis.util;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.ibatis.mapping.MappedStatement;
import top.liujingyanghui.crypto.mybatis.annotation.*;
import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.model.CryptKeyModel;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 加解密公共组件
 *
 * @author : wdh
 * @since : 2022/5/24 16:25
 */
public class MybatisCryptoUtil {

    /**
     * 判断集合中是否含有相同地址的对象
     */
    public static boolean isEqualityListItem(List<Object> alreadyList, Object obj) {
        for (Object item : alreadyList) {
            if (item == obj) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否包含@CryptoClass注解
     */
    public static boolean hasCryptoClass(Object object) {
        CryptoClass sensitiveClass = AnnotationUtil.getAnnotation(object.getClass(), CryptoClass.class);
        return Objects.nonNull(sensitiveClass);
    }

    /**
     * 命名空间获取
     */
    public static String getNamespace(MappedStatement mappedStatements) {
        return mappedStatements.getId().endsWith("_COUNT") ? StrUtil.removeSuffix(mappedStatements.getId(), "_COUNT") : mappedStatements.getId();
    }

    /**
     * 根据命名空间获取Method对象
     */
    public static Method getMethodByNamespace(String namespace) throws ClassNotFoundException {
        String classPath = namespace.substring(0, namespace.lastIndexOf("."));
        String methodName = namespace.substring(namespace.lastIndexOf(".") + 1);
        Class<?> clazz = Class.forName(classPath);
        return ReflectUtil.getMethodByName(clazz, methodName);
    }

    /**
     * 方法获取CryptoKey注解信息
     */
    public static Set<CryptKeyModel> method2MapperCryptoModel(Method method, CryptoMode mode) {
        Set<CryptKeyModel> models = new HashSet<>();
        if (Objects.nonNull(method)) {
            CryptoKeys cryptoKeys = AnnotationUtil.getAnnotation(method, CryptoKeys.class);
            if (Objects.nonNull(cryptoKeys)) {
                setMapperCryptoModel(mode, models, cryptoKeys);
            } else {
                CryptoKey cryptoKey = AnnotationUtil.getAnnotation(method, CryptoKey.class);
                setMapperCryptoModel(mode, models, cryptoKey);
            }
        }
        return models;
    }

    /**
     * 字段获取CryptoKey注解信息
     *
     * @param field 字段
     * @param mode  加解密方式
     * @return MapperCryptoModel
     */
    public static Set<CryptKeyModel> field2MapperCryptoModel(Field field, CryptoMode mode) {
        Set<CryptKeyModel> models = CollUtil.newHashSet();

        CryptoKeys cryptoKeys = field.getAnnotation(CryptoKeys.class);
        if (Objects.nonNull(cryptoKeys)) {
            setMapperCryptoModel(mode, models, cryptoKeys);
        } else {
            CryptoKey cryptoKey = field.getAnnotation(CryptoKey.class);
            setMapperCryptoModel(mode, models, cryptoKey);
        }
        return models;
    }


    private static void setMapperCryptoModel(CryptoMode mode, Set<CryptKeyModel> models, CryptoKeys cryptoKeys) {
        for (CryptoKey cryptoKey : cryptoKeys.value()) {
            if (cryptoKey.mode().equals(CryptoMode.ALL) || cryptoKey.mode().equals(mode)) {
                CryptKeyModel model = new CryptKeyModel();
                model.setField(cryptoKey.key());
                model.setRule(cryptoKey.rule());
                models.add(model);
            }
        }
    }

    private static void setMapperCryptoModel(CryptoMode mode, Set<CryptKeyModel> models, CryptoKey cryptoKey) {
        if (Objects.isNull(cryptoKey)) {
            return;
        }
        if (cryptoKey.mode().equals(CryptoMode.ALL) || cryptoKey.mode().equals(mode)) {
            CryptKeyModel model = new CryptKeyModel();
            model.setField(cryptoKey.key());
            model.setRule(cryptoKey.rule());
            models.add(model);
        }
    }

    /**
     * mappedStatement获取注解信息
     *
     * @param mappedStatement mappedStatement
     * @param mode            加解密模式
     */
    public static Set<CryptKeyModel> mappedStatement2MapperCryptoModel(MappedStatement mappedStatement, CryptoMode mode) throws
            ClassNotFoundException {
        Method method = getMethodByMappedStatement(mappedStatement);
        return method2MapperCryptoModel(method, mode);
    }

    /**
     * 获取Method对象
     */
    public static Method getMethodByMappedStatement(MappedStatement mappedStatement) throws
            ClassNotFoundException {
        String namespace = getNamespace(mappedStatement);
        return getMethodByNamespace(namespace);
    }

    /**
     * 对象深拷贝
     *
     * @param obj       克隆对象
     * @param namespace 对象命名空间
     * @return 克隆后对象
     */
    public static Object objectClone(Object obj, String namespace) {
        Object cloneObj;
        try {
            ByteOutputStream bos = new ByteOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            cloneObj = ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new RuntimeException(namespace + " 方法请求参数克隆失败！请将对象实现Serializable接口");
        }
        return cloneObj;
    }

    /**
     * 根据方式加解密数据
     *
     * @param str        原数据
     * @param cryptoRule 加解密规则
     * @param mode       方式
     * @return 加解密后数据
     */
    public static String cryptoByMode(String str, ICryptoRule cryptoRule, CryptoMode mode) {
        if (CryptoMode.ENCRYPT.equals(mode)) {
            return cryptoRule.encrypt(str);
        } else if (CryptoMode.DECRYPT.equals(mode)) {
            return cryptoRule.decrypt(str);
        } else {
            return str;
        }
    }

    /**
     * 实体对象加密
     *
     * @param paramsObject 实体对象
     * @param mode         加解密模式
     */
    public static <T> T entityCrypto(T paramsObject, CryptoMode mode) throws IllegalAccessException, InstantiationException {
        Class<?> paramObjectClass = paramsObject.getClass();
        Field[] declaredFields = paramObjectClass.getDeclaredFields();
        for (Field field : declaredFields) {
            CryptoString cryptoString = field.getAnnotation(CryptoString.class);
            if (Objects.nonNull(cryptoString)) {
                field.setAccessible(true);
                Object object = field.get(paramsObject);
                if (Objects.isNull(object)) {
                    continue;
                }
                if (object instanceof String) {
                    ICryptoRule cryptoRule = cryptoString.rule().newInstance();
                    field.set(paramsObject, cryptoByMode((String) object, cryptoRule, mode));
                } else if (object instanceof Collection<?>) { // 实体中包含集合
                    @SuppressWarnings("unchecked")
                    Collection<Object> coll = (Collection<Object>) object;
                    if (CollUtil.isNotEmpty(coll)) {
                        Object firstObj = CollUtil.getFirst(coll.iterator());
                        if (!(coll.size() == 1 && Objects.isNull(firstObj)) && firstObj instanceof String) {// 集合泛型为String
                            ICryptoRule cryptoRule = cryptoString.rule().newInstance();
                            Collection<Object> newColl = CollUtil.create(String.class);
                            for (int i = 0; i < coll.size(); i++) {
                                String item = (String) CollUtil.get(coll, i);
                                newColl.add(cryptoByMode(item, cryptoRule, mode));
                            }
                            field.set(paramsObject, newColl);
                        }
                    }
                } else if (object.getClass().isArray()) { // 实体中包含集合
                    Object[] array = (Object[]) object;
                    if (ArrayUtil.isNotEmpty(array)) {
                        if (array[0] instanceof String) {// 集合泛型为String
                            ICryptoRule cryptoRule = cryptoString.rule().newInstance();
                            for (int i = 0; i < array.length; i++) {
                                String item = (String) array[i];
                                array[i] = cryptoByMode(item, cryptoRule, mode);
                            }
                        }
                    }
                }
                continue;
            }

            // 处理实体属性是对象的情况
            CryptoClass cryptoClass = field.getAnnotation(CryptoClass.class);
            if (Objects.nonNull(cryptoClass)) {
                field.setAccessible(true);
                Object object = field.get(paramsObject);
                if (Objects.isNull(object)) {
                    continue;
                }
                if (object instanceof Collection) {
                    Collection<?> coll = (Collection<?>) object;
                    for (Object item : coll) {
                        entityCrypto(item, mode);
                    }
                } else {
                    entityCrypto(object, mode);
                }
            }

            // 实体中有@CryptoKey注解
            Set<CryptKeyModel> models = field2MapperCryptoModel(field, CryptoMode.ENCRYPT);
            if (CollUtil.isNotEmpty(models)) {
                field.setAccessible(true);
                Object object = field.get(paramsObject);
                if (Objects.isNull(object)) {
                    continue;
                }
                if (object instanceof Collection<?>) {
                    Collection<?> coll = (Collection<?>) object;
                    if (CollUtil.isNotEmpty(coll)) {
                        Object firstObj = CollUtil.getFirst(coll);
                        if (!(coll.size() == 1 && Objects.isNull(firstObj)) && firstObj instanceof Map) {
                            paramCrypto(object, models, mode);
                        }
                    }
                } else if (object instanceof Map) {
                    paramCrypto(object, models, mode);
                }
            }
        }
        return paramsObject;
    }

    /**
     * map对象克隆
     *
     * @param map       需要克隆map
     * @param namespace 方法命名空间
     * @return 克隆后map
     */
    public static Object mapClone(Map<String, Object> map, String namespace) {
        HashMap<String, Object> cloneMap = MapUtil.newHashMap();
        cloneMap.putAll(map);
        // 重新克隆入参，防止在后续的业务逻辑中继续使用加密数据从而造成重复加密
        return MybatisCryptoUtil.objectClone(cloneMap, namespace);
    }

    /**
     * 实体对象外的其它数据加解密
     *
     * @param parameterObject 加解密数据
     * @param models          CryptoKey注解信息
     * @param mode            加解密模式
     */
    public static void paramCrypto(Object parameterObject, Set<CryptKeyModel> models, CryptoMode mode) throws InstantiationException, IllegalAccessException {
        List<Object> alreadyList = new ArrayList<>();

        if (parameterObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = ((Map<String, Object>) parameterObject);
            for (Map.Entry<String, Object> mapEntity : map.entrySet()) {
                Object mapEntityValue = mapEntity.getValue();
                if (Objects.isNull(mapEntityValue)) {
                    continue;
                }
                if (mapEntityValue instanceof AbstractWrapper){
                    continue;
                }
                if (isEqualityListItem(alreadyList, mapEntityValue)) {
                    continue;
                }

                alreadyList.add(mapEntityValue);
                if (mapEntityValue instanceof String) {
                    for (CryptKeyModel model : models) {
                        if (model.getField().equals(mapEntity.getKey())) {
                            Class<? extends ICryptoRule> rule = model.getRule();
                            ICryptoRule cryptoRule = rule.newInstance();
                            map.put(model.getField(), cryptoByMode((String) mapEntityValue, cryptoRule, mode));
                            break;
                        }
                    }
                } else {
                    paramCrypto(mapEntityValue, models, mode);
                }
            }
        } else if (parameterObject instanceof Collection) {
            Collection<?> list = (Collection<?>) parameterObject;
            for (Object item : list) {
                paramCrypto(item, models, mode);
            }
        } else {
            CryptoClass cryptoClass = AnnotationUtil.getAnnotation(parameterObject.getClass(), CryptoClass.class);
            if (Objects.nonNull(cryptoClass)) {
                entityCrypto(parameterObject, mode);
            }
        }
    }

    /**
     * 字段加密
     *
     * @param cryptoString 注解
     * @param val          加密字段值
     * @return 加密后的值
     */
    public static Object fieldEncrypt(CryptoString cryptoString, Object val) {
        if (Objects.isNull(cryptoString)) {
            return val;
        }
        if (val instanceof String) {
            try {
                Class<? extends ICryptoRule> rule = cryptoString.rule();
                ICryptoRule cryptoRule = rule.newInstance();
                String value = (String) val;
                return cryptoRule.encrypt(value);
            } catch (Exception e) {
                e.printStackTrace();
                return val;
            }
        } else if (val instanceof Collection) {
            Collection<?> coll = (Collection<?>) val;
            if (CollUtil.isNotEmpty(coll)) {
                Class<? extends ICryptoRule> rule = cryptoString.rule();
                try {
                    ICryptoRule cryptoRule = rule.newInstance();
                    ArrayList<String> resList = CollUtil.newArrayList();
                    for (Object obj : coll) {
                        if (!(obj instanceof String)) {
                            return val;
                        }
                        resList.add(cryptoRule.encrypt((String) obj));
                    }
                    return resList;
                } catch (Exception e) {
                    e.printStackTrace();
                    return val;
                }
            }

        } else if (val.getClass().isArray()) {
            Object[] arr = (Object[]) val;
            if (ArrayUtil.isNotEmpty(arr)) {
                Class<? extends ICryptoRule> rule = cryptoString.rule();
                try {
                    ICryptoRule cryptoRule = rule.newInstance();
                    for (int i = 0; i < arr.length; i++) {
                        if (!(arr[i] instanceof String)) {
                            return val;
                        }
                        arr[i] = cryptoRule.encrypt((String) arr[i]);
                    }
                    return arr;
                } catch (Exception e) {
                    e.printStackTrace();
                    return val;
                }
            }
        }
        return val;
    }

    /**
     * 单个String入参加密处理
     *
     * @param namespace 方法命名空间
     * @param args      参数
     */
    public static void singleStringEncryptHandle(String namespace, Object[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Method method = getMethodByNamespace(namespace);
        CryptoString cryptoString = getCryptoStringByMethod(method, CryptoMode.ENCRYPT);
        if (Objects.isNull(cryptoString)) {
            return;
        }
        if (cryptoString.mode().equals(CryptoMode.DECRYPT)) {
            return;
        }

        ICryptoRule cryptoRule = cryptoString.rule().newInstance();
        args[1] = cryptoRule.encrypt((String) args[1]);
    }

    /**
     * 入参为单个 List<String>、String[]入参加密处理
     *
     * @param cryptoString       加密注解
     * @param parameterObjectMap 参数
     */
    public static void stringsEncryptHandle(CryptoString cryptoString, Map<String, Object> parameterObjectMap) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object mapValue = null;
        for (Map.Entry<String, Object> mapEntity : parameterObjectMap.entrySet()) {
            Object mapEntityValue = mapEntity.getValue();
            if (!(mapEntityValue instanceof Collection || mapEntityValue.getClass().isArray())) {
                return;
            }
            if (Objects.isNull(mapValue)) {
                mapValue = mapEntityValue;
                continue;
            }
            if (mapValue != mapEntityValue) {
                return;
            }
        }
        if (mapValue instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> coll = (Collection<Object>) mapValue;
            if (CollUtil.isEmpty(coll)) {
                return;
            }
            Object first = CollUtil.getFirst(coll);
            if (Objects.isNull(first)){
                return;
            }
            if (!(first instanceof String)) {
                return;
            }
            Collection<String> encryptColl = CollUtil.create(String.class);
            ICryptoRule cryptoRule = cryptoString.rule().newInstance();
            for (Object item : coll) {
                encryptColl.add(cryptoRule.encrypt((String) item));
            }
            parameterObjectMap.replaceAll((k, v) -> encryptColl);
        } else {
            Object[] arr = (Object[]) mapValue;
            if (ArrayUtil.isEmpty(arr)) {
                return;
            }
            if (!(arr[0] instanceof String)) {
                return;
            }
            Object[] encryptArr = new Object[arr.length];
            ICryptoRule cryptoRule = cryptoString.rule().newInstance();
            for (int i = 0; i < arr.length; i++) {
                encryptArr[i] = cryptoRule.encrypt((String) arr[i]);
            }
            parameterObjectMap.replaceAll((k, v) -> encryptArr);
        }
    }

    /**
     * 单个String入参加密处理
     *
     * @param namespace 方法命名空间
     * @param args      参数
     */
    public static void singleEntityEncryptHandle(String namespace, Object[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object paramObject = args[1];
        Class<?> paramObjectClass = paramObject.getClass();
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(paramObjectClass, CryptoClass.class);
        if (Objects.isNull(cryptoClass)) {
            return;
        }

        Object cloneObj = objectClone(paramObject, namespace);
        MybatisCryptoUtil.entityCrypto(cloneObj, CryptoMode.ENCRYPT);
        args[1] = cloneObj;
    }

    /**
     * 获取方法@CryptoString信息
     */
    public static CryptoString getCryptoStringByMethod(Method method, CryptoMode mode) {
        CryptoStrings cryptoStrings = AnnotationUtil.getAnnotation(method, CryptoStrings.class);
        if (Objects.isNull(cryptoStrings)) {
            return AnnotationUtil.getAnnotation(method, CryptoString.class);
        } else {
            for (CryptoString cryptoStringItem : cryptoStrings.value()) {
                if (cryptoStringItem.mode().equals(CryptoMode.ALL) || cryptoStringItem.mode().equals(mode)) {
                    return cryptoStringItem;
                }
            }
        }
        return null;
    }

}
