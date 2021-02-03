package xyz.zzyitj.mybatis.mapper;

import org.apache.ibatis.annotations.*;
import xyz.zzyitj.mybatis.entity.UserEntity;
import xyz.zzyitj.mybatis.entity.UserQuery;

import java.util.List;

public interface UserMapper {

    @Select("select * from user")
    List<UserEntity> getUserPageable(UserQuery query);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into user (name, password) values(#{name}, #{password})")
    int insertUser(UserEntity userEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Update("update user set name = #{name} where id = 14")
    int updateUser(UserEntity userEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Update("delete from user where id = 14")
    int deleteUser(UserEntity userEntity);
}