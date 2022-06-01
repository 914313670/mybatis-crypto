package top.liujingyanghui.crypto.tkmybatis.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * mybatis拦截器配置
 *
 * @author ：wdh
 * @since ：Created in 2022/5/28 16:39
 */
@Configuration
public class MybatisInterceptorAutoConfiguration {
    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addMysqlInterceptor() {
        //创建⾃定义mybatis加密拦截器，添加到chain的最后⾯
        MybatisEncryptInterceptor mybatisEncryptInterceptor = new MybatisEncryptInterceptor();
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            configuration.addInterceptor(mybatisEncryptInterceptor);
        }
    }
}
