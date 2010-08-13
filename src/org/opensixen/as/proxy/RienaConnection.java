package org.opensixen.as.proxy;

import org.compiere.db.CConnection;
import org.compiere.interfaces.Server;
import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.factory.Register;
import org.opensixen.osgi.interfaces.IApplicationServer;
import org.osgi.framework.ServiceReference;

public class RienaConnection extends CConnection {

	private static IRemoteServiceRegistration serviceRegistration;

	private IApplicationServer m_server;
	
	private boolean registered;
	
	public RienaConnection() {
		super(null);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.compiere.db.CConnection#isAppsServerOK(boolean)
	 */
	@Override
	public boolean isAppsServerOK(boolean tryContactAgain) {
		// Si no existe, lo registramos
		if (m_server == null)	{
			getServer();
		}
		// Si sigue sin existir, es que esta mal.
		if (m_server == null)	{
			return false;
		}
		
		return m_server.testConnection();
	}

	/* (non-Javadoc)
	 * @see org.compiere.db.CConnection#getServer()
	 */
	@Override
	public Server getServer() {
		if (m_server != null)	{
			return m_server;
		}
		try {
			unregister();
		}
		catch (Exception e)	{};
		register(getURL());
		ServiceReference ref = Activator.getContext().getServiceReference(IApplicationServer.class.getName());
		IApplicationServer server  = (IApplicationServer) Activator.getContext().getService(ref);
		if (server.testConnection())	{
			m_server = server;
		}
		return m_server;
	}

	
	private String getURL()	{
		StringBuffer buff = new StringBuffer();
		buff.append("http://");
		buff.append(getAppsHost());
		buff.append(":").append(getAppsPort());
		buff.append("/hessian/");
		buff.append(IApplicationServer.path);
		return buff.toString();
		
	}
	
	public static void register(String url )	{
		serviceRegistration = Register.remoteProxy(IApplicationServer.class).usingUrl(url).withProtocol("hessian").andStart(Activator.getContext());
	}
	
	private static void unregister()	{
		serviceRegistration.unregister();
	}

	/* (non-Javadoc)
	 * @see org.compiere.db.CConnection#testAppsServer()
	 */
	@Override
	public synchronized Exception testAppsServer() {
		
		// Borramos el servidor de aplicaciones
		m_server = null;
		try {
			getServer();
		}
		// Si hay alguna excepcion, la devolvemos.
		catch (Exception ex)	{			
			return ex;
		}
		// TODO Auto-generated method stub
		if (m_server == null)	{
			return new RuntimeException("Can't connect with Application Server in "+ getAppsHost()+":"+getAppsPort());
		}

		// TODO Obtener configuracion desde el servidor mediante status.
		
		return null;
	}
	
	
	
}
