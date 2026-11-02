package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.InvalidSearchException;
import com.senac.tsi.MusicApi.exception.RecordLabelNotFoundException;
import com.senac.tsi.MusicApi.exception.ResourceInUseException;
import com.senac.tsi.MusicApi.model.RecordLabel;
import com.senac.tsi.MusicApi.repository.RecordLabelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio das gravadoras
@Service
@Transactional(readOnly = true)
public class RecordLabelService {

    private final RecordLabelRepository repository;

    public RecordLabelService(RecordLabelRepository repository) {
        this.repository = repository;
    }

    public Page<RecordLabel> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public RecordLabel findById(long id) {
        return repository.findById(id).orElseThrow(() -> new RecordLabelNotFoundException(id));
    }

    @Transactional
    public RecordLabel create(RecordLabel label) {
        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria a gravadora com esse id)
        label.setId(0);
        return repository.save(label);
    }

    @Transactional
    public RecordLabel update(long id, RecordLabel data) {
        RecordLabel label = findById(id);
        label.setName(data.getName());
        label.setCountry(data.getCountry());
        label.setFoundedYear(data.getFoundedYear());
        return repository.saveAndFlush(label);
    }

    @Transactional
    public void delete(long id) {
        RecordLabel label = findById(id);
        if (!label.getAlbums().isEmpty())
            throw new ResourceInUseException("Record label with ID: " + id + " still has albums; move them to another label or delete them first");
        repository.delete(label);
    }

    // country tem prioridade; se nenhum dos dois vier, a busca nao faz sentido
    public Page<RecordLabel> search(String country, String name, Pageable pageable) {
        if (country != null && !country.isBlank())
            return repository.findByCountryIgnoreCase(country, pageable);
        if (name != null && !name.isBlank())
            return repository.findByNameContainingIgnoreCase(name, pageable);
        throw new InvalidSearchException("Inform country or name to search record labels");
    }
}
