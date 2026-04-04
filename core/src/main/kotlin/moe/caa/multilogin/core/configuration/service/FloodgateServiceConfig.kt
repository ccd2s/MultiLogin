package moe.caa.multilogin.core.configuration.service

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

class FloodgateServiceConfig(
    id: Int,
    name: String,
    initUUID: InitUUID,
    initNameFormat: String,
    whitelist: Boolean,
    skinRestorer: SkinRestorerConfig
) : BaseServiceConfig(id, name, initUUID, initNameFormat, whitelist, skinRestorer) {

    override fun getServiceType(): ServiceType = ServiceType.FLOODGATE
}
