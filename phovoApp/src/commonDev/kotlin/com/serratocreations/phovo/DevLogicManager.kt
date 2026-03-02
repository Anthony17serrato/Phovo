package com.serratocreations.phovo

abstract class DevLogicManager(

) {
    open suspend fun resetAppState() {
        // common reset logic
    }
}

class DefaultDevLogicManager: DevLogicManager() {

}