package top.zbsong.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
public class User {
    private Integer id;
    private String name;
    private String password;
}
