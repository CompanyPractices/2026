const base_url = "http://localhost:8080";

function fetchApi<T>(route: string): Promise<T> {
    return fetch(base_url + route)
        .then((response) => {
            if (!response.ok) {
                throw new Error(response.status);
            }
            return response.json()
        })
        .catch(error => {throw new Error(`Error status: ${error.message}`)})
}

export default fetchApi;