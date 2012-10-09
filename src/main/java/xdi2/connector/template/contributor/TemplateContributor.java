package xdi2.connector.template.contributor;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.connector.template.api.TemplateApi;
import xdi2.connector.template.mapping.TemplateMapping;
import xdi2.connector.template.util.GraphUtil;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.xri3.impl.XRI3Segment;
import xdi2.messaging.GetOperation;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.ExecutionContext;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.contributor.AbstractContributor;
import xdi2.messaging.target.contributor.ContributorXri;
import xdi2.messaging.target.impl.graph.GraphMessagingTarget;
import xdi2.messaging.target.interceptor.MessageEnvelopeInterceptor;
import xdi2.messaging.target.interceptor.MessagingTargetInterceptor;

@ContributorXri(addresses={"(https://yoursite.com)"})
public class TemplateContributor extends AbstractContributor implements MessagingTargetInterceptor, MessageEnvelopeInterceptor, Prototype<TemplateContributor> {

	private static final Logger log = LoggerFactory.getLogger(TemplateContributor.class);

	private Graph tokenGraph;
	private TemplateApi templateApi;
	private TemplateMapping templateMapping;

	public TemplateContributor() {

		super();

		this.getContributors().addContributor(new TemplateUserContributor());
	}

	/*
	 * Prototype
	 */

	@Override
	public TemplateContributor instanceFor(PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new contributor

		TemplateContributor contributor = new TemplateContributor();

		// set api and mapping

		contributor.setTemplateApi(this.getTemplateApi());
		contributor.setTemplateMapping(this.getTemplateMapping());

		// done

		return contributor;
	}

	/*
	 * MessagingTargetInterceptor
	 */

	@Override
	public void init(MessagingTarget messagingTarget) throws Exception {

		// set the token graph

		if (this.tokenGraph == null && messagingTarget instanceof GraphMessagingTarget) {

			this.setTokenGraph(((GraphMessagingTarget) messagingTarget).getGraph());
		}
	}

	@Override
	public void shutdown(MessagingTarget messagingTarget) throws Exception {

	}

	/*
	 * MessageEnvelopeInterceptor
	 */

	@Override
	public boolean before(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		TemplateContributorExecutionContext.resetUsers(executionContext);

		return false;
	}

	@Override
	public boolean after(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		return false;
	}

	@Override
	public void exception(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext, Exception ex) {

	}

	/*
	 * Sub-Contributors
	 */

	@ContributorXri(addresses={"($)"})
	private class TemplateUserContributor extends AbstractContributor {

		private TemplateUserContributor() {

			super();

			this.getContributors().addContributor(new TemplateUserAttributeContributor());
		}
	}

	@ContributorXri(addresses={"($)"})
	private class TemplateUserAttributeContributor extends AbstractContributor {

		private TemplateUserAttributeContributor() {

			super();
		}

		@Override
		public boolean getContext(XRI3Segment[] contributorXris, XRI3Segment relativeContextNodeXri, XRI3Segment contextNodeXri, GetOperation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

			XRI3Segment templateContextXri = contributorXris[contributorXris.length - 3];
			XRI3Segment userXri = contributorXris[contributorXris.length - 2];
			XRI3Segment templateDataXri = contributorXris[contributorXris.length - 1];

			log.debug("templateContextXri: " + templateContextXri + ", userXri: " + userXri + ", templateDataXri: " + templateDataXri);

			// retrieve the yoursite.com value
			
			String templateValue = null;

			try {

				String templateFieldIdentifier = TemplateContributor.this.templateMapping.templateDataXriToTemplateFieldIdentifier(templateDataXri);
				if (templateFieldIdentifier == null) return false;

				String accessToken = GraphUtil.retrieveAccessToken(TemplateContributor.this.getTokenGraph(), userXri);
				if (accessToken == null) throw new Exception("No access token.");

				JSONObject user = TemplateContributor.this.retrieveUser(executionContext, accessToken);
				if (user == null) throw new Exception("No user.");
				if (! user.has(templateFieldIdentifier)) return false;

				templateValue = user.getString(templateFieldIdentifier);
			} catch (Exception ex) {

				throw new Xdi2MessagingException("Cannot load user data: " + ex.getMessage(), ex, null);
			}

			// add the yoursite.com value to the response

			if (templateValue != null) {

				ContextNode contextNode = messageResult.getGraph().findContextNode(contextNodeXri, true);
				contextNode.createLiteral(templateValue);
			}

			return true;
		}
	}

	/*
	 * Helper methods
	 */

	private JSONObject retrieveUser(ExecutionContext executionContext, String accessToken) throws IOException, JSONException {

		JSONObject user = TemplateContributorExecutionContext.getUser(executionContext, accessToken);

		if (user == null) {

			user = this.templateApi.getUser(accessToken);
			TemplateContributorExecutionContext.putUser(executionContext, accessToken, user);
		}

		return user;
	}
	
	/*
	 * Getters and setters
	 */

	public Graph getTokenGraph() {

		return this.tokenGraph;
	}

	public void setTokenGraph(Graph tokenGraph) {

		this.tokenGraph = tokenGraph;
	}

	public TemplateApi getTemplateApi() {

		return this.templateApi;
	}

	public void setTemplateApi(TemplateApi templateApi) {

		this.templateApi = templateApi;
	}

	public TemplateMapping getTemplateMapping() {
	
		return this.templateMapping;
	}

	public void setTemplateMapping(TemplateMapping templateMapping) {
	
		this.templateMapping = templateMapping;
	}
}
