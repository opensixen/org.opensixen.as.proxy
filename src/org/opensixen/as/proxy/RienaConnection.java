 /******* BEGIN LICENSE BLOCK *****
 * Versión: GPL 2.0/CDDL 1.0/EPL 1.0
 *
 * Los contenidos de este fichero están sujetos a la Licencia
 * Pública General de GNU versión 2.0 (la "Licencia"); no podrá
 * usar este fichero, excepto bajo las condiciones que otorga dicha 
 * Licencia y siempre de acuerdo con el contenido de la presente. 
 * Una copia completa de las condiciones de de dicha licencia,
 * traducida en castellano, deberá estar incluida con el presente
 * programa.
 * 
 * Adicionalmente, puede obtener una copia de la licencia en
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Este fichero es parte del programa opensiXen.
 *
 * OpensiXen es software libre: se puede usar, redistribuir, o
 * modificar; pero siempre bajo los términos de la Licencia 
 * Pública General de GNU, tal y como es publicada por la Free 
 * Software Foundation en su versión 2.0, o a su elección, en 
 * cualquier versión posterior.
 *
 * Este programa se distribuye con la esperanza de que sea útil,
 * pero SIN GARANTÍA ALGUNA; ni siquiera la garantía implícita 
 * MERCANTIL o de APTITUD PARA UN PROPÓSITO DETERMINADO. Consulte 
 * los detalles de la Licencia Pública General GNU para obtener una
 * información más detallada. 
 *
 * TODO EL CÓDIGO PUBLICADO JUNTO CON ESTE FICHERO FORMA PARTE DEL 
 * PROYECTO OPENSIXEN, PUDIENDO O NO ESTAR GOBERNADO POR ESTE MISMO
 * TIPO DE LICENCIA O UNA VARIANTE DE LA MISMA.
 *
 * El desarrollador/es inicial/es del código es
 *  FUNDESLE (Fundación para el desarrollo del Software Libre Empresarial).
 *  Indeos Consultoria S.L. - http://www.indeos.es
 *
 * Contribuyente(s):
 *  Eloy Gómez García <eloy@opensixen.org> 
 *
 * Alternativamente, y a elección del usuario, los contenidos de este
 * fichero podrán ser usados bajo los términos de la Licencia Común del
 * Desarrollo y la Distribución (CDDL) versión 1.0 o posterior; o bajo
 * los términos de la Licencia Pública Eclipse (EPL) versión 1.0. Una 
 * copia completa de las condiciones de dichas licencias, traducida en 
 * castellano, deberán de estar incluidas con el presente programa.
 * Adicionalmente, es posible obtener una copia original de dichas 
 * licencias en su versión original en
 *  http://www.opensource.org/licenses/cddl1.php  y en  
 *  http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * Si el usuario desea el uso de SU versión modificada de este fichero 
 * sólo bajo los términos de una o más de las licencias, y no bajo los 
 * de las otra/s, puede indicar su decisión borrando las menciones a la/s
 * licencia/s sobrantes o no utilizadas por SU versión modificada.
 *
 * Si la presente licencia triple se mantiene íntegra, cualquier usuario 
 * puede utilizar este fichero bajo cualquiera de las tres licencias que 
 * lo gobiernan,  GPL 2.0/CDDL 1.0/EPL 1.0.
 *
 * ***** END LICENSE BLOCK ***** */

package org.opensixen.as.proxy;

import org.compiere.db.CConnection;
import org.compiere.interfaces.Server;
import org.compiere.interfaces.Status;
import org.compiere.util.CLogger;
import org.eclipse.riena.communication.core.IRemoteServiceRegistration;
import org.eclipse.riena.communication.core.factory.Register;
import org.opensixen.osgi.interfaces.IApplicationServer;
import org.osgi.framework.ServiceReference;

/**
 * Riena connection to App Server
 * 
 * 
 * @author Eloy Gomez
 * Indeos Consultoria http://www.indeos.es
 *
 */
public class RienaConnection extends CConnection {

	private CLogger log = CLogger.getCLogger(getClass());
	
	private static IRemoteServiceRegistration serviceRegistration;

	private IApplicationServer m_server;
	
	private boolean registered;
	
	public RienaConnection() {
		super(null);
		// Setup default port
		setAppsPort("8080");
		
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.compiere.db.CConnection#isAppsServerOK(boolean)
	 */
	@Override
	public boolean isAppsServerOK(boolean tryContactAgain) {
		try {
		
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
		catch (Exception e)	{
			return false;
		}
		
		
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
			register(getURL());
			ServiceReference ref = Activator.getContext().getServiceReference(IApplicationServer.class.getName());
			IApplicationServer server  = (IApplicationServer) Activator.getContext().getService(ref);
			if (server.testConnection())	{
				m_server = server;
			}
		}
		catch (Exception e)	{
			log.severe("Can't connect to server.");
		};
		
		return m_server;
	}

	
	private String getURL()	{
		StringBuffer buff = new StringBuffer();
		buff.append("http://");
		buff.append(getAppsHost());
		buff.append(":").append(getAppsPort());
		buff.append("/osx");
		buff.append("/hessian/");
		buff.append(IApplicationServer.path);
		return buff.toString();
		
	}
	
	public static void register(String url )	{
		serviceRegistration = Register.remoteProxy(IApplicationServer.class).usingUrl(url).withProtocol("hessian").andStart(Activator.getContext());
	}
	
	private static void unregister()	{
		if (serviceRegistration != null)	{
			serviceRegistration.unregister();
		}
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
		try {
			updateInfoFromServer(m_server);
		}
		catch (Exception e)	{
			return e;
		}
		
		
		return null;
	}
	
	
	
}
