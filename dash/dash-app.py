import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
import plotly.express as px

import pandas as pd


external_stylesheets = ['https://codepen.io/chriddyp/pen/bWLwgP.css']
app = dash.Dash(__name__, external_stylesheets=external_stylesheets)

# DATA
df = pd.read_parquet('../results/results_preprocessed.parquet')
MEASUREMENTS = ["SHD","LL Score", "dfMM", "dfMM plus", "dfMM minus",
"BDeu Score", "Total time(s)", "BDeu Score_percent", "Total time(s)_percent", "SHD_percent"]


# LAYOUT
app.layout = html.Div([
    # Title
    html.H1('Dashboard ParallelBN Project', style={
            "text-align": "center", "margin-top": "24px", "margin-bottom": "48px"}),
    # Dropdowns
    html.Div([
        html.Label('Network'),
        dcc.Dropdown(
            id="networks-dropdown",
            options=[{"label": network, "value": network}
                     for network in df.network.unique()],
            value="andes",
            multi=False
        ),
        
    ], style={ 'textAlign': "center", "margin-top": "24px", "margin-bottom": "48px"}),
    html.Div([
    html.Label('Y'),
        dcc.Dropdown(
            id='y-dropdown',
            options=[{"label": y, "value": y}
                     for y in MEASUREMENTS],
            value="BDeu Score_percent",
            multi=False
            #labelStyle={'display': 'inline-block'}
        )
    ]),
    # Graphs
    html.H3('Bar Graph Grouped', style={"textAlign": "center"}),
    dcc.Graph(
        id='bar-graph-grouped'
    ),
    html.H3('Bar Graph Total', style={"textAlign": "center"}),
    dcc.Graph(
        id='bar-graph-total'
    )
])


@app.callback(
    #Output('total-visit', 'children'),
    #Output('facebook-visit', 'children'),
    #Output('instagram-visit', 'children'),
    #Output('twitter-visit', 'children'),
    #Output('total-visit-line', 'figure'),
    #Output('total-visit-social-networks-line', 'figure'),
    #Output('world-map', 'figure'),
    Output('bar-graph-grouped', 'figure'),
    Output('bar-graph-total', 'figure'),
    Input('y-dropdown', 'value'),
    Input('networks-dropdown', 'value')
    #Input('date-picker-range', 'end_date'),
    #Input('social-networks-dropdown', 'value'),
    #Input('devices-checkbox', 'value'))
    )
def update_figures(y_selected, network_selected):
    
    print(y_selected)
    print(network_selected)

    # Getting grouped data
    data = df.loc[(df["network"]==network_selected)].groupby(["algorithm", "threads","network", "bbdd", "interleaving"], as_index=False).mean()
    error = df.loc[(df["network"]==network_selected)].groupby(["algorithm", "threads","network", "bbdd", "interleaving"], as_index=False).std()

    x="threads"
    error_y = error[y_selected]
    color = "bbdd"
    network = network_selected

    bar_graph = px.bar(data,
            x=x,
            y=y_selected,
            error_y = error_y,
            color=color,
            barmode="group",
            title="Net: " + network + " - " + y_selected + " vs " + x + " grouped by " + color,
            )

    
    # Total graph
    data = df.loc[(df["network"]==network_selected)].groupby(["algorithm", "threads","network", "interleaving"], as_index=False).mean()
    error = df.loc[(df["network"]==network_selected)].groupby(["algorithm", "threads","network", "interleaving"], as_index=False).std()

    # Parameters
    x="threads"
    error_y = error[y_selected]
    color = "interleaving"

    bar_graph_total= px.bar(data,
                x=x,
                y=y_selected,
                error_y = error_y,
                barmode="group",
                title="Net: " + network + " - " + y_selected + " vs " + x + " grouped by " + color,
                color=color
                )


    
    return bar_graph, bar_graph_total
    """
    total_visit = (
        df
        .loc[(df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
    ).shape[0]

    facebook_visit = (
        df
        .loc[(df.social_network == 'facebook') &
             (df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
    ).shape[0]

    instagram_visit = (
        df
        .loc[(df.social_network == 'instagram') &
             (df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
    ).shape[0]

    twitter_visit = (
        df
        .loc[(df.social_network == 'twitter') &
             (df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
    ).shape[0]

    df_by_month = (
        df
        .loc[(df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
        .groupby(['year', 'month'])
        .count()
        .name
        .reset_index()
        .assign(
            year_month=lambda df: df.year+'-'+df.month
        )
    )

    df_by_month_social_networks = (
        df
        .loc[(df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
        .groupby(['year', 'month', 'social_network'])
        .count()
        .name
        .reset_index()
        .assign(
            year_month=lambda df: df.year+'-'+df.month
        )
    )

    df_country = (
        df
        .loc[(df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
        .groupby(['country_code', 'country'])
        .count()
        .name
        .reset_index()
    )

    df_devices = (
        df
        .loc[(df.social_network.isin(social_networks_selected)) &
             (df.device.isin(devices_selected)) &
             (df.datetime >= start_date_selected) &
             (df.datetime <= end_date_selected)]
        .groupby(['device'])
        .count()
        .name
        .reset_index()
    )

    total_visit_fig = px.line(
        df_by_month,
        x="year_month",
        y="name",
        labels={
            "name": "Total Visits",  "year_month": "Month"
        }
    )

    total_visit_social_network_fig = px.line(
        df_by_month_social_networks,
        x="year_month",
        y="name",
        color="social_network",
        labels={
            "name": "Total Visits",  "year_month": "Month"
        }
    )

    world_map_fig = px.choropleth(
        df_country,
        locations='country_code',
        color="name",
        hover_name="country",
        color_continuous_scale='plasma',
        labels={
            'name': 'Total Visits'
        }
    )

    devices_pie_fig = px.pie(
        df_devices,
        values='name',
        names='device',
        labels={
            'name': 'Total Visits'
        }
    )

    return total_visit, facebook_visit, instagram_visit, twitter_visit, total_visit_fig, total_visit_social_network_fig, world_map_fig, devices_pie_fig
    """

if __name__ == '__main__':
    app.run_server(debug=True)
