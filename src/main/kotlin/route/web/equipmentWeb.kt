package com.bitwiserain.pbbg.route.web

import com.bitwiserain.pbbg.domain.usecase.UserUC
import com.bitwiserain.pbbg.interceptSetUserOrRedirect
import com.bitwiserain.pbbg.memberPageVM
import com.bitwiserain.pbbg.view.equipmentPage
import io.ktor.application.call
import io.ktor.html.respondHtmlTemplate
import io.ktor.locations.Location
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

@Location("/equipment")
class EquipmentLocation

fun Route.equipmentWeb(userUC: UserUC) = route("/equipment") {
    interceptSetUserOrRedirect(userUC)

    get {
        call.respondHtmlTemplate(equipmentPage(call.attributes[memberPageVM])) {}
    }
}
