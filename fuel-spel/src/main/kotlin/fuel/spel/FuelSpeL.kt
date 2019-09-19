package fuel.spel

import java.security.cert.X509Certificate
import javax.net.ssl.*

class FuelSpeL {
}

/**
 * Configure SSL Certificates management.
 *
 * Change trusted managers for provided SSL Context
 *
 */
inline fun String.configSslTrust(trustManagers: Array<TrustManager>? = null) {
    HttpsURLConnection.setDefaultSSLSocketFactory(
            SSLContext.getInstance(this).apply {
                val manager = trustManagers ?: arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
                })

                init(null, manager, java.security.SecureRandom())
            }.socketFactory)
}

/**
 * Set default host verifier.
 *
 * By default (called on `null`) allows all hosts.
 */
inline fun HostnameVerifier?.configAsHostnameVerifier() {
    HttpsURLConnection.setDefaultHostnameVerifier(this ?: HostnameVerifier { _, _ -> true })
}
