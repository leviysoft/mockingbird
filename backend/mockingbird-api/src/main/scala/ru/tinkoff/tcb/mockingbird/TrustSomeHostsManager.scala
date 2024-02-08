package ru.tinkoff.tcb.mockingbird

import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager

/*
  Rewritten from https://github.com/line/armeria/blob/main/core/src/main/java/com/linecorp/armeria/client/IgnoreHostsTrustManager.java
 */
class TrustSomeHostsManager(delegate: X509ExtendedTrustManager, insecureHosts: Set[String])
    extends X509ExtendedTrustManager {
  override def checkServerTrusted(x509Certificates: Array[X509Certificate], authType: String, socket: Socket): Unit =
    if (!insecureHosts.contains(socket.getRemoteSocketAddress.asInstanceOf[InetSocketAddress].getHostString)) {
      delegate.checkServerTrusted(x509Certificates, authType, socket)
    }

  override def checkServerTrusted(
      x509Certificates: Array[X509Certificate],
      authType: String,
      sslEngine: SSLEngine
  ): Unit =
    if (!insecureHosts.contains(sslEngine.getPeerHost)) {
      delegate.checkServerTrusted(x509Certificates, authType, sslEngine)
    }

  override def checkClientTrusted(x509Certificates: Array[X509Certificate], authType: String, socket: Socket): Unit =
    throw new UnsupportedOperationException

  override def checkClientTrusted(
      x509Certificates: Array[X509Certificate],
      authType: String,
      sslEngine: SSLEngine
  ): Unit =
    throw new UnsupportedOperationException

  override def checkClientTrusted(x509Certificates: Array[X509Certificate], authType: String): Unit =
    throw new UnsupportedOperationException

  override def checkServerTrusted(x509Certificates: Array[X509Certificate], authType: String): Unit =
    throw new UnsupportedOperationException

  override def getAcceptedIssuers: Array[X509Certificate] = delegate.getAcceptedIssuers
}

object TrustSomeHostsManager {
  def of(insecureHosts: Set[String]): TrustSomeHostsManager = {
    var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(null.asInstanceOf[KeyStore])
    val trustManagers = trustManagerFactory.getTrustManagers
    for (tm <- trustManagers)
      if (tm.isInstanceOf[X509ExtendedTrustManager])
        return new TrustSomeHostsManager(tm.asInstanceOf[X509ExtendedTrustManager], insecureHosts)

    throw new NoSuchElementException("cannot resolve default trust manager")
  }
}
