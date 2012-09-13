package xdi2.connector.template.contributor;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import xdi2.connector.template.api.TemplateApi;
import xdi2.connector.template.util.GraphUtil;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.xri3.impl.XRI3Segment;
import xdi2.messaging.GetOperation;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.ExecutionContext;
import xdi2.messaging.target.contributor.AbstractContributor;
import xdi2.messaging.target.contributor.ContributorCall;
import xdi2.messaging.target.interceptor.MessageEnvelopeInterceptor;

public class TemplateContributor extends AbstractContributor implements MessageEnvelopeInterceptor {

	private Graph graph;
	private TemplateApi templateApi;

	public TemplateContributor() {

		super();

		this.getContributors().addContributor(new TemplateUserContributor());
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

	@ContributorCall(addresses={"($)"})
	private class TemplateUserContributor extends AbstractContributor {

		private TemplateUserContributor() {

			super();

			this.getContributors().addContributor(new TemplateUserAttributeContributor());
		}
	}

	@ContributorCall(addresses={"$!(gender)","$!(last_name)","$!(first_name)","$!(email)"})
	private class TemplateUserAttributeContributor extends AbstractContributor {

		private TemplateUserAttributeContributor() {

			super();
		}

		@Override
		public boolean getContext(XRI3Segment contributorXri, XRI3Segment relativeContextNodeXri, XRI3Segment contextNodeXri, GetOperation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {
			
			String contributorXriString = contributorXri.toString();
			String literalData = null;

			try {

				String accessToken = GraphUtil.retrieveAccessToken(TemplateContributor.this.getGraph());
				if (accessToken == null) throw new Exception("No access token.");

				JSONObject user = TemplateContributor.this.retrieveUser(executionContext, accessToken);
				if (user == null) throw new Exception("No user.");

				if (contributorXriString.equals("$!(first_name)")) literalData = user.getString("first_name");
				else if (contributorXriString.equals("$!(last_name)")) literalData = user.getString("last_name");
				else if (contributorXriString.equals("$!(gender)")) literalData = user.getString("gender");
				else if (contributorXriString.equals("$!(email)")) literalData = user.getString("email");
				else return false;
			} catch (Exception ex) {

				throw new Xdi2MessagingException("Cannot load user data: " + ex.getMessage(), ex, null);
			}

			if (literalData != null) {

				ContextNode contextNode = messageResult.getGraph().findContextNode(contextNodeXri, true);
				contextNode.createLiteral(literalData);
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

	public Graph getGraph() {

		return this.graph;
	}

	public void setGraph(Graph graph) {

		this.graph = graph;
	}

	public TemplateApi getTemplateApi() {

		return this.templateApi;
	}

	public void setTemplateApi(TemplateApi templateApi) {

		this.templateApi = templateApi;
	}
}
