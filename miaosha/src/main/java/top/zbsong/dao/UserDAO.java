package top.zbsong.dao;

import top.zbsong.entity.User;

public interface UserDAO {
    /**
     * 根据用户id查询用户
     *
     * @param userid
     * @return
     */
    User findById(Integer userid);
}
