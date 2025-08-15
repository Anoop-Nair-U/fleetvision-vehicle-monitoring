package com.anoop.vechile.Telimatry.Repository;


import com.anoop.vechile.Telimatry.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
