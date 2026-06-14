package com.iatechnology.platform.dto;

import com.iatechnology.platform.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private String avatar;
    private Role role;
    // We can also include password if needed for creation, but usually it's omitted in responses
    private String password;
}
