package com.senac.tsi.MusicApi.exception;

import com.senac.tsi.MusicApi.model.ApiKeyRole;

// a chave e valida, mas o nivel de acesso dela nao basta (ex.: USER tentando gerenciar chaves); vira 403
public class InsufficientApiKeyRoleException extends RuntimeException {

    public InsufficientApiKeyRoleException(ApiKeyRole required) {
        super("This operation requires an API key with role " + required);
    }
}
