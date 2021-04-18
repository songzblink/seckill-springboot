package top.zbsong.service;

public interface UserService {
    /**
     * 向redis中写入用户访问次数
     *
     * @param userId
     * @return
     */
    int saveUserCount(Integer userId);

    /**
     * 判断单位时间调用次数
     *
     * @param userId
     * @return
     */
    boolean getUserCount(Integer userId);
}