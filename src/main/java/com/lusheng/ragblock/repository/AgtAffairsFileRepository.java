package com.lusheng.ragblock.repository;

import com.lusheng.ragblock.entity.AgtAffairsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgtAffairsFileRepository extends JpaRepository<AgtAffairsFile, Integer> {

    /**
     * 根据一个标题列表，一次性查询所有匹配的文件记录。
     * Spring Data JPA会根据方法名自动生成 "SELECT ... FROM agt_affairs_files WHERE title IN (...)" 的查询。
     * @param titles 标题列表 (文件名不带扩展名)
     * @return 匹配的文件实体列表
     */
    List<AgtAffairsFile> findByTitleIn(List<String> titles);
}