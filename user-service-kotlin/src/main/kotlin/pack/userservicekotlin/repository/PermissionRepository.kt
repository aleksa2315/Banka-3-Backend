package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.Permission

@Repository
interface PermissionRepository : JpaRepository<Permission, Long>
