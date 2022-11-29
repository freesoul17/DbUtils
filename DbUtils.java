package jdbclab;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class DbUtils {
    /**
     * 获取连接
     */
    public static Connection getConnection() {
        InputStream resource = null;
        try {
            resource = ClassLoader.getSystemClassLoader().getResourceAsStream("jdbc.properties");
            Properties prop = new Properties();
            prop.load(resource);
            String driverClass = prop.getProperty("driverClass");
            String url = prop.getProperty("url");
            String user = prop.getProperty("user");
            String password = prop.getProperty("password");
            Class.forName(driverClass);
            return DriverManager.getConnection(url, user, password);
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(null, null, resource);
        }
        return null;
    }

    /**
     * 关闭Connection
     *
     * @param conn Connection对象
     */
    public static void closeResource(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭PreparedStatement
     *
     * @param pre PreparedStatement对象
     */
    private static void closeResource(PreparedStatement pre) {
        if (pre != null) {
            try {
                pre.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭Connection，PreparedStatement，InputStream
     *
     * @param conn     Connection对象
     * @param pre      PreparedStatement对象
     * @param resource InputStream对象
     */
    private static void closeResource(Connection conn, PreparedStatement pre, InputStream resource) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (pre != null) {
            try {
                pre.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 增加一条记录
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @return 返回影响的行数，若返回-1，则插入失败
     */
    public static int insert(Connection connection, String sql, Object... param) {
        return DbUtils.update(connection, sql, param);
    }

    /**
     * 更新一条记录
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @return 返回影响的行数，若返回-1，则更新失败
     */
    public static int update(Connection connection, String sql, Object... param) {
        PreparedStatement preparedStatement = null;
        int count = -1;
        try {
            preparedStatement = connection.prepareStatement(sql);
            fillParams(preparedStatement, param);
            count = preparedStatement.executeUpdate();
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return count;
    }

    /**
     * 删除一条记录
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @return 返回影响的行数，若返回-1，则插入失败
     */
    public static int delete(Connection connection, String sql, Object... param) {
        return DbUtils.update(connection, sql, param);
    }

    /**
     * 查询任意字段
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @return 返回的结果集，形如[{id=1,name=王五},{id=2,name=李四}],没有记录则返回空[]
     */
    public static List<HashMap<String, Object>> queryObjAsList(Connection connection, String sql, Object... param) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            DbUtils.fillParams(preparedStatement, param);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<HashMap<String, Object>> list = new ArrayList<>();

            ResultSetMetaData metaData = preparedStatement.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    Object columnVal = resultSet.getObject(i + 1);
                    String columnName = metaData.getColumnName(i + 1);
                    hashMap.put(columnName, columnVal);
                }
                list.add(hashMap);
            }

            return list;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return null;
    }

    /**
     * 查询并封装一个bean对象
     *
     * @param clazz      查询bean的class
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @param <T>        查询bean的类型
     * @return 返回一个bean，若没查到返回空
     */
    public static <T> T queryOneBean(Class<T> clazz, Connection connection, String sql, Object... param) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            DbUtils.fillParams(preparedStatement, param);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return fileClass(clazz, preparedStatement, resultSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return null;
    }

    /**
     * 查询并封装多个bean
     *
     * @param clazz      查询bean的class
     * @param connection connection对象
     * @param sql        sql语句
     * @param param      预编译的参数
     * @param <T>        查询bean的类型
     * @return 返回多个bean的集合，若没查到返回空
     */
    public static <T> List<T> queryBeanAsList(Class<T> clazz, Connection connection, String sql, Object... param) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            DbUtils.fillParams(preparedStatement, param);
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData metaData = preparedStatement.getMetaData();
            List<T> list = new ArrayList<>();
            while (resultSet.next()) {
                T t = fileClass(clazz, preparedStatement, resultSet);
                list.add(t);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return null;
    }

    /**
     * 批量插入(带参数）
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param count      执行的次数
     * @param param      预编译的参数
     * @return 返回插入成功的次数，若失败则返回0
     */
    public static int batchInsert(Connection connection, String sql, int count, Object[][] param) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);

            for (int i = 0; i < count; i++) {
                for (int j = 0; j < param[i].length; j++) {
                    preparedStatement.setObject(j + 1, param[i][j]);
                }
                preparedStatement.addBatch();
                if (count > 100000 && i % 1000 == 0) {
                    preparedStatement.executeBatch();
                }
            }
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return 0;
    }

    /**
     * 批量插入（不带参数）
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param count      执行的次数
     * @return 返回插入成功的次数，若失败则返回0
     */
    public static int batchInsert(Connection connection, String sql, int count) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);

            for (int i = 0; i < count; i++) {
                preparedStatement.addBatch();
                if (count > 100000 && i % 1000 == 0) {
                    preparedStatement.executeBatch();
                }
            }
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeResource(preparedStatement);
        }
        return 0;
    }

    /**
     * 批量删除（带参数）
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param count      执行的次数
     * @return 返回删除成功的次数，若失败则返回0
     */
    public static int batchDelete(Connection connection, String sql, int count, Object[][] param) {
        return batchInsert(connection, sql, count, param);
    }

    /**
     * 批量删除（不带参数）
     *
     * @param connection connection对象
     * @param sql        sql语句
     * @param count      执行的次数
     * @return 返回删除成功的次数，若失败则返回0
     */
    @Deprecated
    public static int batchDelete(Connection connection, String sql, int count) {
        return batchInsert(connection, sql, count);
    }

    /**
     * 填充字段
     *
     * @param preparedStatement PreparedStatement对象
     * @param param             填充的参数
     */
    private static void fillParams(PreparedStatement preparedStatement, Object... param) {
        for (int i = 0; i < param.length; i++) {
            try {
                preparedStatement.setObject(i + 1, param[i]);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 填充bean属性
     *
     * @param clazz             bean的class
     * @param preparedStatement PreparedStatement对象
     * @param resultSet         结果集对象
     * @param <T>               bean的类型
     * @return 返回填充好属性的bean
     */
    private static <T> T fileClass(Class<T> clazz, PreparedStatement preparedStatement, ResultSet resultSet) {

        ResultSetMetaData metaData = null;
        int columnCount = 0;
        T t = null;
        try {
            metaData = preparedStatement.getMetaData();
            columnCount = metaData.getColumnCount();
            t = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < columnCount; i++) {
            Object columnVal = null;
            String columnLabel = null;
            Field field = null;
            try {
                columnVal = resultSet.getObject(i + 1);
                columnLabel = metaData.getColumnLabel(i + 1);
                field = clazz.getDeclaredField(columnLabel);
                field.setAccessible(true);
                field.set(t, columnVal);
            } catch (IllegalAccessException | SQLException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return t;
    }

}
