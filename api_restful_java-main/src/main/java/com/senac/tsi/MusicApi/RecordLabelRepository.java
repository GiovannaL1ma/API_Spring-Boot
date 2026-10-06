package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordLabelRepository extends JpaRepository<RecordLabel, Long> {

    Page<RecordLabel> findByCountryIgnoreCase(String country, Pageable pageable);

    Page<RecordLabel> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
