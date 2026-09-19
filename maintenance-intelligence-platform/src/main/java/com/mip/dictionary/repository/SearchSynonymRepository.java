package com.mip.dictionary.repository;

import com.mip.dictionary.entity.SearchSynonym;
import com.mip.dictionary.entity.SynonymDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchSynonymRepository extends JpaRepository<SearchSynonym, Long> {

    Optional<SearchSynonym> findByDomainAndTerm(SynonymDomain domain, String term);

    List<SearchSynonym> findByDomain(SynonymDomain domain);

    List<SearchSynonym> findByTerm(String term);
}
